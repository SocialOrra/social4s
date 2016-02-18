package http.client.request

import http.client.connection.HttpConnection
import http.client.response.{BatchResponse, BatchResponsePart}
import play.api.libs.ws.WSResponse

import scala.collection.mutable.ListBuffer
import scala.concurrent.{ExecutionContext, Future}

abstract class HttpRequestBuilder[C <: HttpConnection[D], D](var requests: ListBuffer[Request[D]] = ListBuffer.empty[Request[D]]) {

  protected def get(relativeUrl: String, queryString: Map[String, Seq[String]], data: Option[D]): Request[D] // GetRequest(relativeUrl, queryString, data)
  protected def post(relativeUrl: String, queryString: Map[String, Seq[String]], data: Option[D], body: Option[String]): Request[D]
  protected def makeParts(data: Option[D], requests:Seq[Request[D]]): Seq[(String, Array[Byte])]
  protected def fromWSResponse(wsResponse: WSResponse): BatchResponse
  protected def maybePaginated(paginated: Boolean, request: Request[D]): Request[D]
  protected def accumulateCompleteRequest(reqRes: (Request[D], BatchResponsePart)): (Request[D], BatchResponsePart)
  protected def newRequestFromIncompleteRequest(reqRes: (Request[D], BatchResponsePart)): Request[D]
  protected def maybeRanged(since: Option[Long], until: Option[Long], request: Request[D]): Request[D]

  def get(relativeUrl: String, queryString: Map[String, Seq[String]], since: Option[Long], until: Option[Long], data: Option[D]): HttpRequestBuilder[C, D] =
    batch(maybeRanged(since, until, get(relativeUrl, queryString, data)))

  def get(relativeUrl: String, queryString: Map[String, Seq[String]], paginate: Boolean, data: Option[D]): HttpRequestBuilder[C, D] =
    batch(maybePaginated(paginate, get(relativeUrl, queryString, data)))

  def post(relativeUrl: String, body: Option[String], queryString: Map[String, Seq[String]], since: Option[Long], until: Option[Long], data: Option[D]): HttpRequestBuilder[C, D] =
    batch(maybeRanged(since, until, post(relativeUrl, queryString, data, body)))

  def post(relativeUrl: String, body: Option[String], queryString: Map[String, Seq[String]], paginated: Boolean, data: Option[D]): HttpRequestBuilder[C, D] =
    batch(maybePaginated(paginated, post(relativeUrl, queryString, data, body)))

  def execute(implicit connection: C, data: Option[D] = None, ec: ExecutionContext): Future[BatchResponse] = {
    val f = connection
      // assemble request parts and send it off
      .batch(makeParts(data, requests))
      // map the response to our internal type
      .map(fromWSResponse)

    postExecute()
    f
  }

  def executeWithPaginationWithoutMerging(implicit facebookConnection: C, data: Option[D] = None, ec: ExecutionContext): Future[Seq[(Request[D], BatchResponsePart)]] = {
    val f = _executeWithPagination(requests)
    postExecute()
    f
  }

  def executeWithPagination(implicit facebookConnection: C, data: Option[D] = None, ec: ExecutionContext): Future[Map[Request[D], Seq[BatchResponsePart]]] = {
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

  private def _executeWithPagination(requests: Seq[Request[D]], completedRequests: Seq[(Request[D], BatchResponsePart)] = Seq.empty)(implicit facebookConnection: C, accessToken: Option[D] = None, ec: ExecutionContext): Future[Seq[(Request[D], BatchResponsePart)]] = {

    val parts = makeParts(accessToken, requests)

    facebookConnection.batch(parts).map { rawResponse ⇒

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

  private def isRequestComplete(reqRes: (Request[D], BatchResponsePart)): Boolean = {
    val request = reqRes._1
    val response = reqRes._2

    if (response.code == 200) {
      request.isComplete(reqRes._2)
    } else {
      // error
      true
    }
  }

  private def batch(request: Request[D]) = {
    requests += request
    this
  }
}

