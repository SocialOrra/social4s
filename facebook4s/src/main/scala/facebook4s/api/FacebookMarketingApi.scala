package facebook4s.api

import facebook4s.request.{FacebookGetRequest, FacebookBatchRequestBuilder}

object FacebookMarketingApi extends FacebookApiHelpers {

  import FacebookApiConstants._

  implicit class FacebookAdsInsightsApiImplicits(requestBuilder: FacebookBatchRequestBuilder) {
    def adInsights(
      adId:        String,
      metric:      Option[String]      = None,
      period:      Option[String]      = None,
      since:       Option[Long]        = None,
      until:       Option[Long]        = None,
      accessToken: Option[AccessToken] = None) = {
      val relativeUrl = buildRelativeUrl(adId, "insights", metric)
      val modifiers = buildModifiers("period" → period)
      requestBuilder.add(FacebookGetRequest(relativeUrl, None, Seq.empty, modifiers, accessToken), since, until)
    }


    def adAccount(
      adAccountId: String,
      accessToken: Option[AccessToken] = None) = {
      val relativeUrl = buildRelativeUrl(s"act_$adAccountId")
      val modifiers = buildModifiers(
        "fields" → Some("name,age,business,created_time,currency,timezone_name"))
      requestBuilder.add(FacebookGetRequest(relativeUrl, None, Seq.empty, modifiers, accessToken), paginated = true)
    }

    def adCampaigns(
      adAccountId: String,
      limit: Option[Int] = None,
      accessToken: Option[AccessToken] = None) = {
      val relativeUrl = buildRelativeUrl(s"act_$adAccountId/campaigns")
      val modifiers = buildModifiers(
        "limit" -> limit,
        "fields" → Some("id,name,account_id,buying_type,adlabels,objective,start_time,stop_time"))
      requestBuilder.add(FacebookGetRequest(relativeUrl, None, Seq.empty, modifiers, accessToken), paginated = true)
    }

    def adCampaign(
      adCampaignId: String,
      accessToken:  Option[AccessToken] = None) = {
      val relativeUrl = buildRelativeUrl(adCampaignId)
      val modifiers = buildModifiers("fields" → Some("name,created_time"))
      requestBuilder.add(FacebookGetRequest(relativeUrl, None, Seq.empty, modifiers, accessToken), paginated = false)
    }
  }
}

