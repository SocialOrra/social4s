package twitter4s.request

import http.client.method.HttpMethod
import http.client.request.{CompletionEvaluation, OrElseCompletionEvaluation, TrueCompletionEvaluation}
import http.client.response.{HttpHeader, HttpResponse}
import play.api.libs.json.{JsSuccess, Json}
import twitter4s.response.TwitterEmptyResponseBodyCompletionEvaluation
import play.api.libs.json.JsError

object TwitterTimelineRequest {
  case class DataId(id: Long)
  implicit val dataIdFmt = Json.format[DataId]
}

case class TwitterTimelineRequest(
  baseUrl:                   String,
  relativeUrl:               String,
  headers:                   Seq[HttpHeader],
  queryString:               Map[String, Seq[String]],
  body:                      Option[Array[Byte]],
  method:                    HttpMethod,
  paginated:                 Boolean,
  authHeaderGen:             (TwitterRequest) ⇒ TwitterAuthorizationHeader,
  customCompletionEvaluator: Option[CompletionEvaluation]                  = None)
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

        // TODO: make sure we remove max_id before adding the new one

        val newQS = queryString + ("max_id" → Seq((s.get.last.id - 1).toString))
        val requestWithnewQS = copy(queryString = newQS)
        requestWithnewQS.copy(
          headers = TwitterRequest.newAuthHeaderForRequest(requestWithnewQS))

      case e: JsError ⇒

        // sometimes it's not an array
        response.json.validate[TwitterTimelineRequest.DataId] match {
          case s1: JsSuccess[TwitterTimelineRequest.DataId] ⇒
            val newQS = queryString + ("max_id" → Seq((s1.get.id - 1).toString))
            val requestWithnewQS = copy(queryString = newQS)
            requestWithnewQS.copy(
              headers = TwitterRequest.newAuthHeaderForRequest(requestWithnewQS))

          case e1: JsError ⇒
            // TODO: how do we handle this case?
            println(s"OH NO! Could not find data with an id in \n${response.json.toString}\n Caused by JsError.toJson(e).toString")
            val newQS = queryString + ("max_id" → Seq(0.toString))
            val requestWithnewQS = copy(queryString = newQS)
            requestWithnewQS.copy(
              headers = TwitterRequest.newAuthHeaderForRequest(requestWithnewQS))
        }
    }
  }

  override protected def withoutHeader(httpHeaderName: String): TwitterRequest = {
    copy(headers = headers.filterNot(_.name == httpHeaderName))
  }
}
