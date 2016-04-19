package twitter4s.request

import http.client.method.HttpMethod
import http.client.request.{CompletionEvaluation, OrElseCompletionEvaluation, TrueCompletionEvaluation}
import http.client.response.{HttpHeader, HttpResponse}
import twitter4s.response.TwitterEmptyNextCursorCompletionEvaluation

case class TwitterCursoredRequest(
  relativeUrl: String,
  headers:     Seq[HttpHeader],
  queryString: Map[String, Seq[String]],
  body:        Option[Array[Byte]],
  method:      HttpMethod, paginated: Boolean,
  customCompletionEvaluator: Option[CompletionEvaluation] = None)
    extends TwitterRequest {

  override val completionEvaluator = if (paginated) {
    customCompletionEvaluator match {
      case Some(c) ⇒ OrElseCompletionEvaluation(customCompletionEvaluator.get, TrueCompletionEvaluation)
      case _       ⇒ TwitterEmptyNextCursorCompletionEvaluation
    }
  } else TrueCompletionEvaluation

  override def nextRequest(response: HttpResponse): TwitterRequest = {
    val next = (response.json \ "next_cursor").validate[Long].get
    val newQS = queryString + ("cursor" → Seq(next.toString))
    copy(queryString = newQS)
  }
}
