package twitter4s.api

import http.client.method.GetMethod
import http.client.request.{HttpRequestBuilderCallback, HttpRequestHelpers}
import twitter4s.request._

import scala.concurrent.ExecutionContext

object TwitterApi extends HttpRequestHelpers {

  val baseUrl = "https://api.twitter.com"
  val apiVersionUrl = "1.1"

  implicit class TwitterApiImplicits(requestBuilder: TwitterRequestBuilder) {

    def userTimeline(screenName: String, maxId: Option[Long] = None)(implicit partCompletionCallback: HttpRequestBuilderCallback, ec: ExecutionContext, authHeaderGen: (TwitterRequest) ⇒ TwitterAuthorizationHeader) = {

      val queryString = Map(
        "screen_name" → Seq(screenName),
        "exclude_replies" → Seq("false"),
        "trim_user" → Seq("true"),
        "include_rts" → Seq("true"))
      val relativeUrl = buildRelativeUrl("/", apiVersionUrl, "statuses", "user_timeline.json")
      val headers = Seq.empty

      val request = TwitterTimelineRequest(
        baseUrl = baseUrl,
        relativeUrl = relativeUrl,
        headers = headers,
        method = GetMethod,
        queryString = queryString,
        body = None,
        paginated = true,
        authHeaderGen = authHeaderGen)

      val authHeader = authHeaderGen(request)

      val authRequest = request.copy(
        headers = headers ++ Seq(authHeader))

      requestBuilder.makeRequest(authRequest, partCompletionCallback)
    }

    def userTimelineByUserId(userId: String)(implicit partCompletionCallback: HttpRequestBuilderCallback, ec: ExecutionContext, authHeaderGen: (TwitterRequest) ⇒ TwitterAuthorizationHeader) = {

      val queryString = Map(
        "user_id" → Seq(userId),
        "exclude_replies" → Seq("false"),
        "trim_user" → Seq("true"),
        "include_rts" → Seq("true"))
      val relativeUrl = buildRelativeUrl("/", apiVersionUrl, "statuses", "user_timeline.json")
      val headers = Seq.empty

      val request = TwitterTimelineRequest(
        baseUrl = baseUrl,
        relativeUrl = relativeUrl,
        headers = headers,
        method = GetMethod,
        queryString = queryString,
        body = None,
        paginated = true,
        authHeaderGen = authHeaderGen)

      val authHeader = authHeaderGen(request)

      val authRequest = request.copy(
        headers = headers ++ Seq(authHeader))

      requestBuilder.makeRequest(authRequest, partCompletionCallback)
    }

    def adAccount(adAccountId: String)(implicit partCompletionCallback: HttpRequestBuilderCallback, ec: ExecutionContext, authHeaderGen: (TwitterRequest) ⇒ TwitterAuthorizationHeader) = {

      val queryString = Map.empty[String, Seq[String]]
      val relativeUrl = buildRelativeUrl("/", apiVersionUrl, "accounts", adAccountId)
      val headers = Seq.empty

      val request = TwitterTimelineRequest(
        baseUrl = baseUrl,
        relativeUrl = relativeUrl,
        headers = headers,
        method = GetMethod,
        queryString = queryString,
        body = None,
        paginated = false,
        authHeaderGen = authHeaderGen)

      val authHeader = authHeaderGen(request)

      val authRequest = request.copy(
        headers = headers ++ Seq(authHeader))

      requestBuilder.makeRequest(authRequest, partCompletionCallback)
    }

    def campaigns(adAccountId: String)(implicit partCompletionCallback: HttpRequestBuilderCallback, ec: ExecutionContext, authHeaderGen: (TwitterRequest) ⇒ TwitterAuthorizationHeader) = {

      val queryString = Map.empty[String, Seq[String]]
      val relativeUrl = buildRelativeUrl("/", apiVersionUrl, "accounts", adAccountId, "campaigns")
      val headers = Seq.empty

      val request = TwitterCursoredRequest(
        baseUrl = baseUrl,
        relativeUrl = relativeUrl,
        headers = headers,
        method = GetMethod,
        queryString = queryString,
        body = None,
        paginated = true,
        authHeaderGen = authHeaderGen)

      val authHeader = authHeaderGen(request)

      val authRequest = request.copy(
        headers = headers ++ Seq(authHeader))

      requestBuilder.makeRequest(authRequest, partCompletionCallback)
    }

    def lineItems(adAccountId: String)(implicit partCompletionCallback: HttpRequestBuilderCallback, ec: ExecutionContext, authHeaderGen: (TwitterRequest) ⇒ TwitterAuthorizationHeader) = {

      val queryString = Map.empty[String, Seq[String]]
      val relativeUrl = buildRelativeUrl("/", apiVersionUrl, "accounts", adAccountId, "line_items")
      val headers = Seq.empty

      val request = TwitterCursoredRequest(
        baseUrl = baseUrl,
        relativeUrl = relativeUrl,
        headers = headers,
        method = GetMethod,
        queryString = queryString,
        body = None,
        paginated = true,
        authHeaderGen = authHeaderGen)

      val authHeader = authHeaderGen(request)

      val authRequest = request.copy(
        headers = headers ++ Seq(authHeader))

      requestBuilder.makeRequest(authRequest, partCompletionCallback)
    }

    def tweet(tweetId: String)(implicit partCompletionCallback: HttpRequestBuilderCallback, ec: ExecutionContext, authHeaderGen: (TwitterRequest) ⇒ TwitterAuthorizationHeader) = {

      val queryString = Map.empty[String, Seq[String]]
      val relativeUrl = buildRelativeUrl("/", apiVersionUrl, "statuses", "show", s"$tweetId.json")
      val headers = Seq.empty

      val request = TwitterTimelineRequest(
        baseUrl = baseUrl,
        relativeUrl = relativeUrl,
        headers = headers,
        method = GetMethod,
        queryString = queryString,
        body = None,
        paginated = true,
        authHeaderGen = authHeaderGen)

      val authHeader = authHeaderGen(request)

      val authRequest = request.copy(
        headers = headers ++ Seq(authHeader))

      requestBuilder.makeRequest(authRequest, partCompletionCallback)
    }

    def promotedAccounts(adAccountId: String)(implicit partCompletionCallback: HttpRequestBuilderCallback, ec: ExecutionContext, authHeaderGen: (TwitterRequest) ⇒ TwitterAuthorizationHeader) = {

      val queryString = Map.empty[String, Seq[String]]
      val relativeUrl = buildRelativeUrl("/", apiVersionUrl, "accounts", adAccountId, "promoted_accounts")
      val headers = Seq.empty

      val request = TwitterCursoredRequest(
        baseUrl = baseUrl,
        relativeUrl = relativeUrl,
        headers = headers,
        method = GetMethod,
        queryString = queryString,
        body = None,
        paginated = true,
        authHeaderGen = authHeaderGen)

      val authHeader = authHeaderGen(request)

      val authRequest = request.copy(
        headers = headers ++ Seq(authHeader))

      requestBuilder.makeRequest(authRequest, partCompletionCallback)
    }

    def promotedTweets(adAccountId: String)(implicit partCompletionCallback: HttpRequestBuilderCallback, ec: ExecutionContext, authHeaderGen: (TwitterRequest) ⇒ TwitterAuthorizationHeader) = {

      val queryString = Map.empty[String, Seq[String]]
      val relativeUrl = buildRelativeUrl("/", apiVersionUrl, "accounts", adAccountId, "promoted_tweets")
      val headers = Seq.empty

      val request = TwitterCursoredRequest(
        baseUrl = baseUrl,
        relativeUrl = relativeUrl,
        headers = headers,
        method = GetMethod,
        queryString = queryString,
        body = None,
        paginated = true,
        authHeaderGen = authHeaderGen)

      val authHeader = authHeaderGen(request)

      val authRequest = request.copy(
        headers = headers ++ Seq(authHeader))

      requestBuilder.makeRequest(authRequest, partCompletionCallback)
    }

    def lineItemStatsDaily(lineItemId: String, adAccountId: String, startTime: String, endTime: String)(implicit partCompletionCallback: HttpRequestBuilderCallback, ec: ExecutionContext, authHeaderGen: (TwitterRequest) ⇒ TwitterAuthorizationHeader) = {

      val queryString = Map(
        "entity" → Seq("PROMOTED_TWEET"),
        "entity_ids" → Seq(lineItemId),
        "start_time" → Seq(startTime),
        "end_time" → Seq(endTime),
        "metric_groups" → Seq("ENGAGEMENT,BILLING,MEDIA,WEB_CONVERSION,MOBILE_CONVERSION,VIDEO"),
        "placement" → Seq("ALL_ON_TWITTER"),
        "granularity" → Seq("TOTAL"))

      val relativeUrl = buildRelativeUrl("/", apiVersionUrl, "stats", "accounts", adAccountId)
      val headers = Seq.empty

      val request = TwitterCursoredRequest(
        baseUrl = baseUrl,
        relativeUrl = relativeUrl,
        headers = headers,
        method = GetMethod,
        queryString = queryString,
        body = None,
        paginated = true,
        authHeaderGen = authHeaderGen)

      val authHeader = authHeaderGen(request)

      val authRequest = request.copy(
        headers = headers ++ Seq(authHeader))

      requestBuilder.makeRequest(authRequest, partCompletionCallback)
    }

    def promotedTweetStatsDaily(adAccountId: String, promotedTweetIds: Seq[String], startTime: String, endTime: String)(implicit partCompletionCallback: HttpRequestBuilderCallback, ec: ExecutionContext, authHeaderGen: (TwitterRequest) ⇒ TwitterAuthorizationHeader) = {
      val queryString = Map(
        "entity" → Seq("PROMOTED_TWEET"),
        "entity_ids" → promotedTweetIds,
        "start_time" → Seq(startTime),
        "end_time" → Seq(endTime),
        "metric_groups" → Seq("WEB_CONVERSION,ENGAGEMENT,BILLING,MEDIA"),
        "placement" → Seq("ALL_ON_TWITTER"),
        "granularity" → Seq("DAY"))

      val relativeUrl = buildRelativeUrl("/", apiVersionUrl, "stats", "accounts", adAccountId)
      val headers = Seq.empty

      val request = TwitterCursoredRequest(
        baseUrl = baseUrl,
        relativeUrl = relativeUrl,
        headers = headers,
        method = GetMethod,
        queryString = queryString,
        body = None,
        paginated = true,
        authHeaderGen = authHeaderGen)

      val authHeader = authHeaderGen(request)

      val authRequest = request.copy(
        headers = headers ++ Seq(authHeader))

      requestBuilder.makeRequest(authRequest, partCompletionCallback)
    }

    def user(userId: String)(implicit partCompletionCallback: HttpRequestBuilderCallback, ec: ExecutionContext, authHeaderGen: (TwitterRequest) ⇒ TwitterAuthorizationHeader) = {

      val queryString = Map("user_id" → Seq(userId))
      val relativeUrl = buildRelativeUrl("/", apiVersionUrl, "users", "show.json")
      val headers = Seq.empty

      val request = TwitterTimelineRequest(
        baseUrl = baseUrl,
        relativeUrl = relativeUrl,
        headers = headers,
        method = GetMethod,
        queryString = queryString,
        body = None,
        paginated = true,
        authHeaderGen = authHeaderGen)

      val authHeader = authHeaderGen(request)

      val authRequest = request.copy(
        headers = headers ++ Seq(authHeader))

      requestBuilder.makeRequest(authRequest, partCompletionCallback)
    }

    def mentionsTimeline(implicit partCompletionCallback: HttpRequestBuilderCallback, ec: ExecutionContext, authHeaderGen: (TwitterRequest) ⇒ TwitterAuthorizationHeader) = {

      val queryString = Map.empty[String, Seq[String]]
      val relativeUrl = buildRelativeUrl("/", apiVersionUrl, "statuses", "mentions_timeline.json")
      val headers = Seq.empty

      val request = TwitterTimelineRequest(
        baseUrl = baseUrl,
        relativeUrl = relativeUrl,
        headers = headers,
        method = GetMethod,
        queryString = queryString,
        body = None,
        paginated = true,
        authHeaderGen = authHeaderGen)

      val authHeader = authHeaderGen(request)

      val authRequest = request.copy(
        headers = headers ++ Seq(authHeader))

      requestBuilder.makeRequest(authRequest, partCompletionCallback)
    }

  }
}
