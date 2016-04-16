package twitter4s

import http.client.connection.HttpConnection
import http.client.method.HttpMethod
import http.client.request.{CompletionEvaluation, Request}
import http.client.response.{HttpHeader, HttpResponse}
import play.api.libs.json.{JsSuccess, Json}

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.ExecutionContext.Implicits.global

object TwitterEmptyNextCursorCompletionEvaluation extends CompletionEvaluation {
  override def apply(request: Request, response: HttpResponse): Boolean = {
    (response.json \ "next_cursor").validate[Long] match {
      case s: JsSuccess[Long] if s.get > 0 ⇒ false
      case _                               ⇒ true
    }
  }
}

object TwitterPaginatedRequest {

  def apply(
    relativeUrl: String,
    headers:     Seq[HttpHeader],
    queryString: Map[String, Seq[String]],
    body:        Option[Array[Byte]],
    method:      HttpMethod
  ): TwitterPaginatedRequest = {
    TwitterPaginatedRequest(
      relativeUrl,
      headers,
      queryString,
      body,
      method,
      TwitterEmptyNextCursorCompletionEvaluation
    )
  }

  def apply(request: Request): TwitterPaginatedRequest = {
    TwitterPaginatedRequest(
      relativeUrl = request.relativeUrl,
      headers = request.headers,
      queryString = request.queryString,
      body = request.body,
      method = request.method,
      completionEvaluator = TwitterEmptyNextCursorCompletionEvaluation
    )
  }
}
case class TwitterPaginatedRequest(relativeUrl: String, headers: Seq[HttpHeader], queryString: Map[String, Seq[String]], body: Option[Array[Byte]], method: HttpMethod, completionEvaluator: CompletionEvaluation) extends Request {
  override def toJson(extraQueryStringParams: Map[String, Seq[String]]): String = "{}"
}

class TwitterRequestBuilder(connection: HttpConnection) {

  def shutdown() = connection.shutdown()

  def makeRequest(request: Request, since: Option[Long], until: Option[Long]): Future[(Request, Seq[HttpResponse])] = {
    val r = maybeRanged(since, until, request)
    executeWithPagination(r)
  }

  def makeRequest(request: Request, paginated: Boolean): Future[(Request, Seq[HttpResponse])] = {
    val r = maybePaginated(paginated, request)
    executeWithPagination(r)
  }

  def executeWithPagination(request: Request)(implicit ec: ExecutionContext): Future[(Request, Seq[HttpResponse])] = {
    val f = _executeWithPagination(request) map { requestAndResponseParts ⇒
      // TODO: could there be no parts?
      val request = requestAndResponseParts._1
      val parts = requestAndResponseParts._2
      (request, parts)
    }
    f
  }

  private def _executeWithPagination(request: Request, completedResponseParts: Seq[HttpResponse] = Seq.empty)(implicit ec: ExecutionContext): Future[(Request, Seq[HttpResponse])] = {

    val responseF = connection.makeRequest(request)

    responseF.map { response ⇒

      val newRequest = if (!isRequestComplete(request, response))
        Some(newRequestFromIncompleteRequest(request, response))
      else None

      (response, newRequest)

    } flatMap {
      case (responsePart, Some(newRequest)) ⇒
        _executeWithPagination(newRequest, completedResponseParts ++ Seq(responsePart))
      case (responsePart, None) ⇒
        Future.successful { (request, completedResponseParts ++ Seq(responsePart)) }
    }
  }

  private def isRequestComplete(request: Request, response: HttpResponse): Boolean = {

    if (response.status == 200) {
      // this only works for paginated requests with cursors where we want to scroll until the end
      // TODO: need to support since / until as well, so this code will move elsewhere
      request.isComplete(response)
      true
    } else {
      // error
      true
    }
  }

  protected def newRequestFromIncompleteRequest(request: Request, response: HttpResponse): Request = ???
  protected def maybeRanged(since: Option[Long], until: Option[Long], request: Request): Request = request // todo: implement

  protected def maybePaginated(paginated: Boolean, request: Request): Request = {
    if (paginated) TwitterPaginatedRequest(request)
    else request
  }
}

