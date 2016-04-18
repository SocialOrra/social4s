package http.client.request

import http.client.response.HttpResponse

trait CompletionEvaluation extends ((Request, HttpResponse) ⇒ Boolean)

object TrueCompletionEvaluation extends CompletionEvaluation {
  override def apply(request: Request, response: HttpResponse): Boolean = true
}
