package facebook4s.api

import facebook4s.request.{ FacebookGetRequest, FacebookBatchRequestBuilder }

object FacebookMarketingApi extends FacebookApiHelpers {

  import FacebookApiConstants._

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

    def adAccountCampaigns(
      adAccountId: String,
      limit: Option[Int] = Some(DEFAULT_API_LIMIT),
      accessToken: Option[AccessToken] = None) = {
      val relativeUrl = buildRelativeUrl(s"act_$adAccountId", "campaigns")
      val modifiers = buildModifiers("limit" -> limit)
      requestBuilder.get(FacebookGetRequest(relativeUrl, modifiers, accessToken), paginate = true)
    }

    def adCampaign(
      adCampaignId: String,
      accessToken: Option[AccessToken] = None) = {
      val relativeUrl = buildRelativeUrl(adCampaignId)
      val modifiers = buildModifiers("fields" -> Some("name,created_time"))
      requestBuilder.get(FacebookGetRequest(relativeUrl, modifiers, accessToken), paginate = false)
    }
  }
}

