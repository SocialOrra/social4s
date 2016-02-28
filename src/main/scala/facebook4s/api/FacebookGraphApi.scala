package facebook4s.api

import facebook4s.request.{ FacebookGetRequest, FacebookBatchRequestBuilder }

object FacebookGraphApi {

  implicit class FacebookGraphApi(requestBuilder: FacebookBatchRequestBuilder) {
    def me(fields: Map[String, Seq[String]] = Map.empty, accessToken: Option[AccessToken] = None) = requestBuilder.get(FacebookGetRequest("me", Map.empty, accessToken), since = None, until = None)
    def friends(fields: Map[String, Seq[String]] = Map.empty, accessToken: Option[AccessToken] = None) = requestBuilder.get(FacebookGetRequest("me/friends", fields, accessToken), since = None, until = None)
    def albums(fields: Map[String, Seq[String]] = Map.empty, paginate: Boolean = false, accessToken: Option[AccessToken] = None) = requestBuilder.get(FacebookGetRequest("me/albums", fields, accessToken), paginate)
  }
}
