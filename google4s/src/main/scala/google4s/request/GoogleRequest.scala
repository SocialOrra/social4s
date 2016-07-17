package google4s.request

import http.client.method.HttpMethod
import http.client.request._
import http.client.response.{HttpHeader, HttpResponse}
import play.api.libs.json.JsSuccess

case class GoogleRequest(
  override val baseUrl:      String,
  override val relativeUrl:  String,
  override val method:       HttpMethod,
  override val headers:      Seq[HttpHeader]              = Seq.empty,
  _queryString:              Map[String, Seq[String]]     = Map.empty,
  override val body:         Option[Array[Byte]]          = None,
  accessToken:               String,
  paginated:                 Boolean                      = false,
  customCompletionEvaluator: Option[CompletionEvaluation] = None)
    extends PaginatedHttpRequest[GoogleRequest] {

  // TODO: make accessToken an option
  override val queryString = if (accessToken.isEmpty) _queryString else _queryString + ("access_token" → Seq(accessToken))
  override def toJson(extraQueryStringParams: Map[String, Seq[String]]): String = "{}"

  override val completionEvaluator = if (paginated) {
    customCompletionEvaluator match {
      case Some(c) ⇒ OrElseCompletionEvaluation(customCompletionEvaluator.get, TrueCompletionEvaluation)
      case _       ⇒ GoogleEmptyNextLinkCompletionEvaluation
    }
  } else TrueCompletionEvaluation

  override def nextRequest(response: HttpResponse): GoogleRequest = {
    (response.json \ "nextLink").validate[String] match {
      case s: JsSuccess[String] ⇒
        copy(relativeUrl = s.get)
      case _ ⇒
        println(s"OH NO! Could not find nextLink in ${response.json.toString}")
        ???
    }
  }
}
