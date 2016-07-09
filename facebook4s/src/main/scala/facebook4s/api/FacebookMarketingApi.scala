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

    def adAccountCampaigns(
      adAccountId: String,
      limit:       Option[Int]         = Some(DEFAULT_API_LIMIT),
      accessToken: Option[AccessToken] = None) = {
      val relativeUrl = buildRelativeUrl(s"act_$adAccountId", "campaigns")
      val modifiers = buildModifiers("limit" → limit)
      requestBuilder.add(FacebookGetRequest(relativeUrl, None, Seq.empty, modifiers, accessToken), paginated = true)
    }

    def adAccountData(
      adAccountId: String,
      limit:       Option[Int]         = Some(DEFAULT_API_LIMIT),
      accessToken: Option[AccessToken] = None) = {
      val relativeUrl = buildRelativeUrl(s"act_$adAccountId")
      val modifiers = buildModifiers(
        "limit" → limit,
        "fields" → Some("name,age,business,created_time,currency,timezone_name," +
          "campaigns{objective,id,adlabels,recommendations,start_time,stop_time,updated_time,ads," +
          "adsets{billing_event,bid_amount,bid_info,created_time,end_time,start_time,optimization_goal," +
          "ads{id,conversion_specs,configured_status,tracking_specs,creative{id,thumbnail_url,object_type,object_story_spec},name,insights}}}"))
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

