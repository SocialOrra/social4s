package http.client.request

import http.client.connection.HttpConnection
import http.client.response.{ BatchResponse, BatchResponsePart }
import play.api.libs.ws.WSResponse

import scala.collection.mutable.ListBuffer
import scala.concurrent.{ ExecutionContext, Future }

abstract class HttpBatchRequestBuilder(var requests: ListBuffer[Request] = ListBuffer.empty[Request], connection: HttpConnection, batchUrl: String) {

  protected def makeParts(requests: Seq[Request]): Seq[(String, Array[Byte])]
  protected def fromWSResponse(wsResponse: WSResponse): BatchResponse
  protected def maybePaginated(paginated: Boolean, request: Request): Request
  protected def accumulateCompleteRequest(reqRes: (Request, BatchResponsePart)): (Request, BatchResponsePart)
  protected def newRequestFromIncompleteRequest(reqRes: (Request, BatchResponsePart)): Request
  protected def maybeRanged(since: Option[Long], until: Option[Long], request: Request): Request

  def shutdown() = connection.shutdown()

  def get(getRequest: GetRequest, since: Option[Long], until: Option[Long]): HttpBatchRequestBuilder =
    batch(maybeRanged(since, until, getRequest))

  def get(getRequest: GetRequest, paginate: Boolean): HttpBatchRequestBuilder =
    batch(maybePaginated(paginate, getRequest))

  def post[T](postRequest: PostRequest[T], since: Option[Long], until: Option[Long]): HttpBatchRequestBuilder =
    batch(maybeRanged(since, until, postRequest))

  def post[T](postRequest: PostRequest[T], paginated: Boolean): HttpBatchRequestBuilder =
    batch(maybePaginated(paginated, postRequest))

  def execute(implicit ec: ExecutionContext): Future[BatchResponse] = {
    val f = connection
      // assemble request parts and send it off
      .batch(BatchRequest(batchUrl, makeParts(requests)))
      // map the response to our internal type
      .map(fromWSResponse)

    postExecute()
    f
  }

  def executeWithPaginationWithoutMerging(implicit ec: ExecutionContext): Future[Seq[(Request, BatchResponsePart)]] = {
    val f = _executeWithPagination(requests)
    postExecute()
    f
  }

  def executeWithPagination(implicit ec: ExecutionContext): Future[Map[Request, Seq[BatchResponsePart]]] = {
    val f = _executeWithPagination(requests) map { requestsAndResponses ⇒
      requestsAndResponses
        .groupBy(_._1)
        .mapValues(_.map(_._2))
        .map { requestAndResponseParts ⇒
          // TODO: could there be no parts?
          val request = requestAndResponseParts._1
          val parts = requestAndResponseParts._2
          //val combinedBody: String = parts.map(p ⇒ p.bodyJson.validate[JsObject].get).foldLeft(JsObject(Seq.empty))(_ deepMerge _).toString()
          //val combinedPart = BatchResponsePart(code = parts.head.code, headers = parts.head.headers, body = combinedBody)
          (request, parts)
        }
    }

    postExecute()
    f
  }

  private def postExecute(): Unit = {
    requests = ListBuffer.empty
  }

  private def _executeWithPagination(requests: Seq[Request], completedRequests: Seq[(Request, BatchResponsePart)] = Seq.empty)(implicit ec: ExecutionContext): Future[Seq[(Request, BatchResponsePart)]] = {

    val parts = makeParts(requests)

    connection.batch(BatchRequest(batchUrl, parts)).map { rawResponse ⇒

      val response = fromWSResponse(rawResponse)

      val responses = requests
        // group up each request with it's corresponding response
        .zip(response.parts)
        // create two groups: complete, incomplete
        // note that  non-200 responses are considered complete
        .groupBy(isRequestComplete)

      val complete = responses.getOrElse(true, Seq.empty).map(accumulateCompleteRequest)
      val incompleteResponses = responses.getOrElse(false, Seq.empty).map(accumulateCompleteRequest)
      val newRequests = responses.getOrElse(false, Seq.empty).map(newRequestFromIncompleteRequest)

      (complete ++ incompleteResponses, newRequests)

    } flatMap {
      case (complete, incomplete) if incomplete.nonEmpty ⇒
        _executeWithPagination(incomplete, completedRequests ++ complete)
      case (complete, _) ⇒
        Future.successful { completedRequests ++ complete }
    }
  }

  private def isRequestComplete(reqRes: (Request, BatchResponsePart)): Boolean = {
    val request = reqRes._1
    val response = reqRes._2

    if (response.code == 200) {
      request.isComplete(reqRes._2)
    } else {
      // error
      true
    }
  }

  private def batch(request: Request) = {
    requests += request
    this
  }
}

