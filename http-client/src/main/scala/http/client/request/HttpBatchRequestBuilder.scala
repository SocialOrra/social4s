package http.client.request

import http.client.connection.HttpConnection
import http.client.method.HttpMethod
import http.client.response.{BatchResponse, BatchResponsePart, HttpResponse}

import scala.collection.mutable.ListBuffer
import scala.concurrent.{ExecutionContext, Future}

abstract class HttpBatchRequestBuilder[
  GRequest <: GetRequest,
  PRequest <: PostRequest[_],
  BResponse <: BatchResponse[BResponsePart],
  BResponsePart <: BatchResponsePart,
  BRequestBuilder <: HttpBatchRequestBuilder[GRequest, PRequest, BResponse, BResponsePart, BRequestBuilder]]
  (var requests: ListBuffer[Request] = ListBuffer.empty[Request], connection: HttpConnection, batchUrl: String) {

  protected def makeBatchRequestBody(requests: Seq[Request]): Array[Byte]
  protected def makeBatchRequest(batchUrl: String, body: Array[Byte]): PostRequest[Array[Byte]]
  protected def fromHttpResponse(response: HttpResponse): BResponse
  protected def maybePaginated(paginated: Boolean, request: Request): Request
  protected def accumulateCompleteRequest(reqRes: (Request, BResponsePart)): (Request, BResponsePart)
  protected def newRequestFromIncompleteRequest(reqRes: (Request, BResponsePart)): Request
  protected def maybeRanged(since: Option[Long], until: Option[Long], request: Request): Request

  def shutdown() = connection.shutdown()

  def get(getRequest: GRequest, since: Option[Long], until: Option[Long]): Unit =
    batch(maybeRanged(since, until, getRequest))

  def get(getRequest: GRequest, paginate: Boolean): Unit =
    batch(maybePaginated(paginate, getRequest))

  def post[T](postRequest: PRequest, since: Option[Long], until: Option[Long]): Unit =
    batch(maybeRanged(since, until, postRequest))

  def post[T](postRequest: PRequest, paginated: Boolean): Unit =
    batch(maybePaginated(paginated, postRequest))

  def execute(implicit ec: ExecutionContext): Future[BResponse] = {
    // assemble request parts
    val body = makeBatchRequestBody(requests)
    val postRequest = makeBatchRequest(batchUrl, body)
    // and send it off
    val f = connection.post(postRequest)
      // map the response to our internal type
      .map(fromHttpResponse)

    postExecute()
    f
  }

  def executeWithPaginationWithoutMerging(implicit ec: ExecutionContext): Future[Seq[(Request, BResponsePart)]] = {
    val f = _executeWithPagination(requests)
    postExecute()
    f
  }

  def executeWithPagination(implicit ec: ExecutionContext): Future[Map[Request, Seq[BResponsePart]]] = {
    val f = _executeWithPagination(requests) map { requestsAndResponses ⇒
      requestsAndResponses
        // group response parts by request
        .groupBy(_._1)
        // remove grouping key, leave (request,responseParts)
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

  private def _executeWithPagination(requests: Seq[Request], completedRequests: Seq[(Request, BResponsePart)] = Seq.empty)(implicit ec: ExecutionContext): Future[Seq[(Request, BResponsePart)]] = {

    val body = makeBatchRequestBody(requests)
    val postRequest = makeBatchRequest(batchUrl, body)

    connection.post(postRequest).map { rawResponse =>

      val response = fromHttpResponse(rawResponse)

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

  private def isRequestComplete(reqRes: (Request, BResponsePart)): Boolean = {
    val request = reqRes._1
    val response = reqRes._2

    if (response.code == 200) {
      request.isComplete(reqRes._2)
    } else {
      // error
      true
    }
  }

  private def batch(request: Request): Unit = {
    requests += request
  }
}

