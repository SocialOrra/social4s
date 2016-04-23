package google4s.request

import http.client.request.{CompletionEvaluation, Request}
import http.client.response.HttpResponse
import play.api.libs.json.JsSuccess

object GoogleEmptyNextLinkCompletionEvaluation extends CompletionEvaluation {
  override def apply(request: Request, response: HttpResponse): Boolean = {
    (response.json \ "nextLink").validate[String] match {
      case s: JsSuccess[String] if s.get.length > 0 ⇒ false
      case _                                        ⇒ true
    }
  }
}
