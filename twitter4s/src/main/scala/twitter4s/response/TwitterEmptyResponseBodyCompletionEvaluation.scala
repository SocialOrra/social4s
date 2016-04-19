package twitter4s.response

import http.client.request.{CompletionEvaluation, Request}
import http.client.response.HttpResponse

object TwitterEmptyResponseBodyCompletionEvaluation extends CompletionEvaluation {
  override def apply(request: Request, response: HttpResponse): Boolean = {
    response.body.isEmpty
  }
}
