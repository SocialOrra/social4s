package http.client.request

import http.client.response.BatchResponsePart

trait CompletionEvaluation extends ((Request[_], BatchResponsePart) ⇒ Boolean)

class TrueCompletionEvaluation extends CompletionEvaluation {
  override def apply(request: Request[_], response: BatchResponsePart): Boolean = true
}
