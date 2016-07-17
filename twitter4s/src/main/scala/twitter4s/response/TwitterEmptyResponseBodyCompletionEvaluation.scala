package twitter4s.response

import http.client.request.{CompletionEvaluation, HttpRequest}
import http.client.response.HttpResponse

object TwitterEmptyResponseBodyCompletionEvaluation extends CompletionEvaluation {
  override def apply(request: HttpRequest, response: HttpResponse): Boolean = {
    // TODO: leave this as such or turn parse it as Json and validate that it is an empty array?
    response.body.equals("[]")
  }
}
