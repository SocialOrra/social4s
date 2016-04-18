package twitter4s

import http.client.connection.HttpConnection
import http.client.method.HttpMethod
import http.client.request.{CompletionEvaluation, Request, TrueCompletionEvaluation}
import http.client.response.{HttpHeader, HttpResponse}
import play.api.libs.json.JsSuccess

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.ExecutionContext.Implicits.global

/** Keeps scrolling so long as the next_cursor is greater than zero.
 */
object TwitterEmptyNextCursorCompletionEvaluation extends CompletionEvaluation {
  override def apply(request: Request, response: HttpResponse): Boolean = {
    (response.json \ "next_cursor").validate[Long] match {
      case s: JsSuccess[Long] if s.get > 0L ⇒ false
      case _                                ⇒ true
    }
  }
}

object TwitterRequest {

  def apply(request: Request): TwitterRequest = {
    TwitterRequest(
      relativeUrl = request.relativeUrl,
      headers = request.headers,
      queryString = request.queryString,
      body = request.body,
      method = request.method
    )
  }
}
case class TwitterRequest(relativeUrl: String, headers: Seq[HttpHeader], queryString: Map[String, Seq[String]], body: Option[Array[Byte]], method: HttpMethod, paginated: Boolean = false) extends Request {
  // TODO: set this based on whether we're paginated or not
  override val completionEvaluator = if (paginated) TwitterEmptyNextCursorCompletionEvaluation else TrueCompletionEvaluation
  override def toJson(extraQueryStringParams: Map[String, Seq[String]]): String = "{}"
}

class TwitterRequestBuilder(connection: HttpConnection) {

  def shutdown() = connection.shutdown()

  def makeRequest(request: TwitterRequest, since: Option[Long], until: Option[Long]): Future[(Request, Seq[HttpResponse])] = {
    val r = maybeRanged(since, until, request)
    executeWithPagination(r)
  }

  def makeRequest(request: TwitterRequest, paginated: Boolean): Future[(Request, Seq[HttpResponse])] = {
    val r = maybePaginated(paginated, request)
    executeWithPagination(r)
  }

  def executeWithPagination(request: TwitterRequest)(implicit ec: ExecutionContext): Future[(Request, Seq[HttpResponse])] = {
    val f = _executeWithPagination(request) map { requestAndResponseParts ⇒
      // TODO: could there be no parts?
      val request = requestAndResponseParts._1
      val parts = requestAndResponseParts._2
      (request, parts)
    }
    f
  }

  private def _executeWithPagination(request: TwitterRequest, completedResponseParts: Seq[HttpResponse] = Seq.empty)(implicit ec: ExecutionContext): Future[(Request, Seq[HttpResponse])] = {

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

  private def isRequestComplete(request: TwitterRequest, response: HttpResponse): Boolean = {

    if (response.status == 200) {
      // this only works for paginated requests with cursors where we want to scroll until the end
      // TODO: need to support since / until as well, so this code will move elsewhere
      //       since / until can be implemented via a custom completion evaluator that takes user input into account
      request.isComplete(response)
    } else {
      // error
      true
    }
  }

  protected def newRequestFromIncompleteRequest(request: TwitterRequest, response: HttpResponse): TwitterRequest = {
    val next = (response.json \ "next_cursor").validate[Long].get
    val newQS = request.queryString + ("cursor" → Seq(next.toString))
    request.copy(queryString = newQS)
    request
  }

  protected def maybeRanged(since: Option[Long], until: Option[Long], request: TwitterRequest): TwitterRequest = request // todo: implement

  protected def maybePaginated(paginated: Boolean, request: TwitterRequest): TwitterRequest = {
    // TODO: this has no effect ATM, we need a way to turn pagination on and off
    //if (paginated) TwitterRequest(request)
    //else request
    request
  }
}

