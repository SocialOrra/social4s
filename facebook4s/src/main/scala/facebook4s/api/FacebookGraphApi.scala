package facebook4s.api

import facebook4s.request.{FacebookGetRequest, FacebookBatchRequestBuilder}

object FacebookGraphApi {

  implicit class FacebookGraphApi(requestBuilder: FacebookBatchRequestBuilder) {
    def me(fields: Map[String, Seq[String]] = Map.empty, accessToken: Option[AccessToken] = None) = requestBuilder.add(FacebookGetRequest("me", None, Seq.empty, Map.empty, accessToken), since = None, until = None)
    def friends(fields: Map[String, Seq[String]] = Map.empty, accessToken: Option[AccessToken] = None) = requestBuilder.add(FacebookGetRequest("me/friends", None, Seq.empty, fields, accessToken), since = None, until = None)
    def albums(fields: Map[String, Seq[String]] = Map.empty, paginate: Boolean = false, accessToken: Option[AccessToken] = None) = requestBuilder.add(FacebookGetRequest("me/albums", None, Seq.empty, fields, accessToken), paginate)
  }
}
