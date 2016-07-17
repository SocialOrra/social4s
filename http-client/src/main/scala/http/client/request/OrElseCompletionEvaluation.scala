package http.client.request

import http.client.response.HttpResponse

/** Created by hisham on 4/19/16.
 */
case class OrElseCompletionEvaluation(ce1: CompletionEvaluation, ce2: CompletionEvaluation) extends CompletionEvaluation {
  override def apply(v1: HttpRequest, v2: HttpResponse): Boolean = {
    ce1.apply(v1, v2) || ce2.apply(v1, v2)
  }
}
