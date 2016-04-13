package twitter4s

import http.client.connection.HttpConnection
import http.client.request.{GetRequest, PostRequest, Request}
import http.client.response.{BatchResponsePart, HttpResponse}

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.libs.json.JsValue

trait TwitterResponsePart extends BatchResponsePart

trait TwitterResponse extends HttpResponse {
  val code: Int
}

abstract class TwitterRequestBuilder(connection: HttpConnection, batchUrl: String) {

  protected def fromHttpResponse(response: HttpResponse): TwitterResponsePart
  protected def maybePaginated[T <: Request](paginated: Boolean, request: T): T
  protected def accumulateCompleteRequest(request: Request, response: TwitterResponsePart): (Request, TwitterResponsePart)
  protected def newRequestFromIncompleteRequest(request: Request, response: TwitterResponsePart): Request
  protected def maybeRanged[T <: Request](since: Option[Long], until: Option[Long], request: T): T

  def shutdown() = connection.shutdown()

  def get(getRequest: GetRequest, since: Option[Long], until: Option[Long]): Future[TwitterResponse] = {
    val r = maybeRanged(since, until, getRequest)
    executeWithPagination(r)
  }

  def get(getRequest: GetRequest, paginate: Boolean): Future[TwitterResponse] = {
    val r = maybePaginated(paginate, getRequest)
    executeWithPagination(r)
  }

  def post[T](postRequest: PostRequest[T], since: Option[Long], until: Option[Long]): Future[TwitterResponse] = {
    val r = maybeRanged(since, until, postRequest)
    executeWithPagination(r)
  }

  def post[T](postRequest: PostRequest[T], paginated: Boolean)(implicit ec: ExecutionContext): Future[TwitterResponse] = {
    val r = maybePaginated(paginated, postRequest)
    executeWithPagination(r)
  }

  def executeWithPagination(request: Request)(implicit ec: ExecutionContext): Future[TwitterResponse] = {
    val f = _executeWithPagination(request) map { requestsAndResponses ⇒
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
          //val combinedPart = TwitterResponsePart(code = parts.head.code, headers = parts.head.headers, body = combinedBody)
          (request, parts)
        }
    }

    postExecute()
    f

    ???
  }

  private def postExecute(): Unit = {
  }

  private def _executeWithPagination(request: Request, completedRequests: Seq[(Request, TwitterResponsePart)] = Seq.empty)(implicit ec: ExecutionContext): Future[Seq[(Request, TwitterResponsePart)]] = {

    val f: Future[HttpResponse] = if (request.isInstanceOf[GetRequest]) connection.get(request.asInstanceOf[GetRequest])
    else connection.post(request.asInstanceOf[PostRequest[Array[Byte]]])

    f.map { rawResponse =>

      val response = fromHttpResponse(rawResponse)

      val complete = accumulateCompleteRequest(request, response)

      val newRequest = if (!isRequestComplete(request, response))
        Some(newRequestFromIncompleteRequest(request, response))
      else None

      (complete, newRequest)

    } flatMap {
      case (complete, Some(newRequest)) ⇒
        //_executeWithPagination(newRequest, completedRequests ++ complete)
        ???
      case (complete, None) ⇒
        Future.successful { completedRequests /*++ complete*/ }
    }
  }

  private def isRequestComplete(request: Request, response: TwitterResponsePart): Boolean = {

    if (response.code== 200) {
      request.isComplete(response)
    } else {
      // error
      true
    }
  }
}

