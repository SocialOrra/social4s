package http.client.request

import http.client.response.HttpResponse

trait CompletionEvaluation extends ((HttpRequest, HttpResponse) ⇒ Boolean)

object TrueCompletionEvaluation extends CompletionEvaluation {
  override def apply(request: HttpRequest, response: HttpResponse): Boolean = true
}
