package twitter4s.api

import http.client.method.GetMethod
import http.client.request.{HttpRequestHelpers, HttpRequest}
import twitter4s.request._

import scala.concurrent.ExecutionContext

object TwitterApi extends HttpRequestHelpers {

  val baseUrl = "https://api.twitter.com"
  val apiVersionUrl = "1.1"

  implicit class TwitterApiImplicits(requestBuilder: TwitterRequestBuilder) {

    def userTimeline(screenName: String, maxId: Option[Long] = None)(implicit partCompletionCallback: TwitterRequestBuilderCallback, ec: ExecutionContext, authHeaderGen: (TwitterRequest) ⇒ TwitterAuthorizationHeader) = {

      val queryString = Map("screen_name" → Seq(screenName))
      val relativeUrl = buildRelativeUrl("/", apiVersionUrl, "/statuses/user_timeline.json")
      val headers = Seq.empty

      val request = TwitterTimelineRequest(
        baseUrl = baseUrl,
        relativeUrl = relativeUrl,
        headers = headers,
        method = GetMethod,
        queryString = queryString,
        body = None,
        paginated = true)

      val authHeader = authHeaderGen(request)

      val authRequest = request.copy(
        headers = headers ++ Seq(authHeader))

      requestBuilder.makeRequest(authRequest, partCompletionCallback)
    }
  }
}
