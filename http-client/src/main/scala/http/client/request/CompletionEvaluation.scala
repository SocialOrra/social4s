package http.client.request

import http.client.response.BatchResponsePart

trait CompletionEvaluation extends ((Request, BatchResponsePart) ⇒ Boolean)

class TrueCompletionEvaluation extends CompletionEvaluation {
  override def apply(request: Request, response: BatchResponsePart): Boolean = true
}
