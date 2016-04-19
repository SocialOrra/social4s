package twitter4s.response

import http.client.request.{CompletionEvaluation, Request}
import http.client.response.HttpResponse
import play.api.libs.json.JsSuccess

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
