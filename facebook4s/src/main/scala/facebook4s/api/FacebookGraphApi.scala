package facebook4s.api

import facebook4s.request.{FacebookBatchRequestBuilder, FacebookGetRequest}
import http.client.request.HttpRequestHelpers

object FacebookGraphApi extends HttpRequestHelpers {

  import FacebookApiConstants._

  implicit class FacebookGraphApiImplicits(requestBuilder: FacebookBatchRequestBuilder) {
    def me(fields: Map[String, Seq[String]] = Map.empty, accessToken: Option[AccessToken] = None) = requestBuilder.add(FacebookGetRequest("me", None, Seq.empty, Map.empty, accessToken), since = None, until = None)
    def friends(fields: Map[String, Seq[String]] = Map.empty, accessToken: Option[AccessToken] = None) = requestBuilder.add(FacebookGetRequest("me/friends", None, Seq.empty, fields, accessToken), since = None, until = None)
    def albums(fields: Map[String, Seq[String]] = Map.empty, paginate: Boolean = false, accessToken: Option[AccessToken] = None) = requestBuilder.add(FacebookGetRequest("me/albums", None, Seq.empty, fields, accessToken), paginate)

    def pageInsights(
      pageId:      String,
      metric:      Option[String]      = None,
      period:      Option[String]      = None,
      since:       Option[Long]        = None,
      until:       Option[Long]        = None,
      accessToken: Option[AccessToken] = None) = {
      val relativeUrl = buildRelativeUrl(pageId, "insights", metric)
      val modifiers = buildModifiers(
        "period" → period)
      requestBuilder.add(FacebookGetRequest(relativeUrl, None, Seq.empty, modifiers, accessToken), since, until)
    }

    def pagePosts(
      pageId:      String,
      period:      Option[String]      = None,
      since:       Option[Long]        = None,
      until:       Option[Long]        = None,
      accessToken: Option[AccessToken] = None) = {
      val relativeUrl = buildRelativeUrl(pageId, "posts")
      val modifiers = buildModifiers(
        "period" → period,
        "fields" → Some("message,link,id,call_to_action,attachments,created_time"))
      requestBuilder.add(FacebookGetRequest(relativeUrl, None, Seq.empty, modifiers, accessToken), since, until)
    }
  }
}
