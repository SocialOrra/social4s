package facebook4s.api

import facebook4s.request.FacebookRequestBuilder

object FacebookGraphApi {

  implicit class FacebookGraphApi(requestBuilder: FacebookRequestBuilder) {
    def me(fields: Map[String, Seq[String]] = Map.empty, accessToken: Option[AccessToken] = None) = requestBuilder.get("me", Map.empty, since = None, until = None, accessToken)
    def friends(fields: Map[String, Seq[String]] = Map.empty, accessToken: Option[AccessToken] = None) = requestBuilder.get("me/friends", fields, since = None, until = None, accessToken)
  }
}
