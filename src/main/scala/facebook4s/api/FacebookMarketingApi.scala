package facebook4s.api

import facebook4s.request.{ FacebookGetRequest, FacebookBatchRequestBuilder }

object FacebookMarketingApi extends FacebookApiHelpers {

  implicit class FacebookAdsInsightsApi(requestBuilder: FacebookBatchRequestBuilder) {
    def adInsights(
      adId: String,
      metric: Option[String] = None,
      period: Option[String] = None,
      since: Option[Long] = None,
      until: Option[Long] = None,
      accessToken: Option[AccessToken] = None) = {
      val relativeUrl = buildRelativeUrl(adId, "insights", metric)
      val modifiers = buildModifiers("period" -> period)
      requestBuilder.get(FacebookGetRequest(relativeUrl, modifiers, accessToken), since, until)
    }
  }
}

