package twitter4s.response

import http.client.request.{CompletionEvaluation, Request}
import http.client.response.HttpResponse

object TwitterEmptyResponseBodyCompletionEvaluation extends CompletionEvaluation {
  override def apply(request: Request, response: HttpResponse): Boolean = {
    // TODO: leave this as such or turn parse it as Json and validate that it is an empty array?
    response.body.equals("[]")
  }
}
