package facebook4s.api

import facebook4s.request.FacebookRequestBuilder

object FacebookMarketingApi extends FacebookApiHelpers {

  implicit class FacebookAdsInsightsApi(requestBuilder: FacebookRequestBuilder) {
    def adInsights(
      adId: String,
      metric: Option[String] = None,
      period: Option[String] = None,
      since: Option[Long] = None,
      until: Option[Long] = None,
      accessToken: Option[AccessToken] = None): FacebookRequestBuilder = {

      val relativeUrl = buildRelativeUrl(adId, "insights", metric)
      val modifiers = buildModifiers("period" -> period)
      requestBuilder.get(relativeUrl, modifiers, since, until, accessToken)
    }
  }
}

