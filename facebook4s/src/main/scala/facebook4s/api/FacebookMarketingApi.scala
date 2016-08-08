package facebook4s.api

import facebook4s.request.{FacebookBatchRequestBuilder, FacebookGetRequest}
import http.client.request.HttpRequestHelpers

object FacebookMarketingApi extends HttpRequestHelpers {

  import FacebookApiConstants._

  implicit class FacebookAdsInsightsApiImplicits(requestBuilder: FacebookBatchRequestBuilder) {

    def adAccount(
      adAccountId: String,
      accessToken: Option[AccessToken] = None) = {
      val relativeUrl = buildRelativeUrl("v2.6", s"act_$adAccountId")
      val modifiers = buildModifiers(
        "fields" → Some("name,age,business,created_time,currency,timezone_name,account_id"))
      requestBuilder.add(FacebookGetRequest(relativeUrl, None, Seq.empty, modifiers, accessToken), paginated = true)
    }

    def adCampaign(
      adCampaignId: String,
      accessToken:  Option[AccessToken] = None) = {
      val relativeUrl = buildRelativeUrl("v2.6", adCampaignId)
      val modifiers = buildModifiers("fields" → Some("name,created_time"))
      requestBuilder.add(FacebookGetRequest(relativeUrl, None, Seq.empty, modifiers, accessToken), paginated = false)
    }

    def adCampaigns(
      adAccountId: String,
      limit:       Option[Int]         = None,
      accessToken: Option[AccessToken] = None) = {
      val relativeUrl = buildRelativeUrl("v2.6", s"act_$adAccountId", "campaigns")
      val modifiers = buildModifiers(
        "limit" → limit,
        "fields" → Some("id,name,account_id,buying_type,adlabels,objective,start_time,stop_time"))
      requestBuilder.add(FacebookGetRequest(relativeUrl, None, Seq.empty, modifiers, accessToken), paginated = true)
    }

    def adSets(
      campaignId:  String,
      limit:       Option[Int]         = None,
      accessToken: Option[AccessToken] = None) = {
      val relativeUrl = buildRelativeUrl("v2.6", campaignId, "adsets")
      val modifiers = buildModifiers(
        "limit" → limit,
        "fields" → Some("id,name,account_id,start_time,budget_remaining,created_time,bid_amount,billing_event,configured_status,daily_budget,effective_status"))
      requestBuilder.add(FacebookGetRequest(relativeUrl, None, Seq.empty, modifiers, accessToken), paginated = true)
    }

    def ads(
      adSetId:     String,
      limit:       Option[Int]         = None,
      accessToken: Option[AccessToken] = None) = {
      val relativeUrl = buildRelativeUrl("v2.6", adSetId, "ads")
      val modifiers = buildModifiers(
        "limit" → limit,
        "fields" → Some("adlabels,bid_amount,bid_info,bid_type,configured_status,conversion_specs,name,tracking_specs,created_time,adset_id,effective_status,status,creative{id,thumbnail_url,object_type,object_story_spec}"))
      requestBuilder.add(FacebookGetRequest(relativeUrl, None, Seq.empty, modifiers, accessToken), paginated = true)
    }

    def adInsights(
      adId:        String,
      metric:      Option[String]      = None,
      period:      Option[String]      = None,
      since:       Option[Long]        = None,
      until:       Option[Long]        = None,
      accessToken: Option[AccessToken] = None) = {
      val relativeUrl = buildRelativeUrl("v2.6", adId, "insights", metric)
      val modifiers = buildModifiers(
        "period" → period,
        "time_increment" → Some("1"))
      requestBuilder.add(FacebookGetRequest(relativeUrl, None, Seq.empty, modifiers, accessToken), since, until)
    }

  }
}

