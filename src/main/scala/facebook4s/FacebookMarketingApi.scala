package facebook4s

object FacebookMarketingApi {

  implicit class FacebookAdsInsightsApi(requestBuilder: FacebookRequestBuilder) {
    def adInsights(adId: String, accessToken: Option[AccessToken] = None) = requestBuilder.get(s"$adId/insights", Map.empty, accessToken)
    def campaignInsights(campaignId: String, accessToken: Option[AccessToken] = None) = requestBuilder.get(s"$campaignId/insights", Map.empty, accessToken)
    def adSetInsights(adSetId: String, accessToken: Option[AccessToken] = None) = requestBuilder.get(s"$adSetId/insights", Map.empty, accessToken)
  }
}
