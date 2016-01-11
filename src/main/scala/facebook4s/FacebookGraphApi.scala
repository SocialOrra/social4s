package facebook4s

object FacebookGraphApi {

  implicit class FacebookGraphApi(requestBuilder: FacebookRequestBuilder) {
    def me(fields: Map[String, Seq[String]] = Map.empty, accessToken: Option[AccessToken] = None) = requestBuilder.get("me", Map.empty, accessToken)
    def friends(fields: Map[String, Seq[String]] = Map.empty, accessToken: Option[AccessToken] = None) = requestBuilder.get("me/friends", fields, accessToken)
    def pageInsights(pageId: String, fields: Map[String, Seq[String]] = Map.empty, accessToken: Option[AccessToken] = None) = requestBuilder.get("me/friends", fields, accessToken)
  }
}
