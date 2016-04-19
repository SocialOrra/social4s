package twitter4s.request

import http.client.method.HttpMethod
import http.client.request.{CompletionEvaluation, OrElseCompletionEvaluation, TrueCompletionEvaluation}
import http.client.response.{HttpHeader, HttpResponse}
import play.api.libs.json.{JsSuccess, Json}
import twitter4s.response.TwitterEmptyResponseBodyCompletionEvaluation

object TwitterTimelineRequest {
  case class DataId(id: Long)
  implicit val dataIdFmt = Json.format[DataId]
}

case class TwitterTimelineRequest(
  relativeUrl:               String,
  headers:                   Seq[HttpHeader],
  queryString:               Map[String, Seq[String]],
  body:                      Option[Array[Byte]],
  method:                    HttpMethod,
  paginated:                 Boolean,
  customCompletionEvaluator: Option[CompletionEvaluation] = None)
    extends TwitterRequest {

  override val completionEvaluator = if (paginated) {
    customCompletionEvaluator match {
      case Some(c) ⇒ OrElseCompletionEvaluation(customCompletionEvaluator.get, TrueCompletionEvaluation)
      case _       ⇒ TwitterEmptyResponseBodyCompletionEvaluation
    }
  } else TrueCompletionEvaluation

  override def nextRequest(response: HttpResponse): TwitterRequest = {
    // take last item in data, take it's ID, subtract 1 from it, and set it as max_id.
    response.json.validate[Array[TwitterTimelineRequest.DataId]] match {
      case s: JsSuccess[Array[TwitterTimelineRequest.DataId]] ⇒
        // we subtract 1 in order not to re-include the last item in the timeline
        val newQS = queryString + ("max_id" → Seq((s.get.last.id - 1).toString))
        copy(queryString = newQS)
      case _ ⇒
        // TODO: how do we handle this case?
        println(s"OH NO! Could not find data with an id in ${response.json.toString}")
        val newQS = queryString + ("max_id" → Seq(0.toString))
        copy(queryString = newQS)
    }
  }
}
