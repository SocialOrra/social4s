package twitter4s

import com.typesafe.config.ConfigFactory
import http.client.connection.impl.PlayWSHttpConnection
import http.client.method.GetMethod
import http.client.response.HttpHeader
import org.scalatest._
import twitter4s.request.{TwitterAuthorizationHeader, TwitterTimelineRequest}

import scala.concurrent.duration._
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global

class TwitterRequestSpec extends FlatSpec with Matchers with OptionValues with Inside with Inspectors {

  val config = ConfigFactory.load("test.conf")
  val _baseUrl = config.getString("twitter4s.test.base-url")
  val _relativeUrl = config.getString("twitter4s.test.relative-url")
  val _headers = Seq.empty[HttpHeader]
  val _queryString = Map("screen_name" → Seq("codewarrior"))

  val oauthConsumerSecret = config.getString("twitter4s.test.oauth-consumer-secret")
  val oauthConsumerKey = config.getString("twitter4s.test.oauth-consumer-key")
  val oauthToken = config.getString("twitter4s.test.oauth-token")
  val oauthTokenSecret = config.getString("twitter4s.test.oauth-token-secret")

  val twAuthHeaderGen = TwitterAuthorizationHeader.generate(
    oauthConsumerKey = oauthConsumerKey,
    oauthToken = oauthToken,
    oauthConsumerSecret = oauthConsumerSecret,
    oauthTokenSecret = oauthTokenSecret)(_)

  val request = TwitterTimelineRequest(
    baseUrl = _baseUrl,
    relativeUrl = _relativeUrl,
    headers = _headers,
    method = GetMethod,
    queryString = _queryString,
    body = None,
    paginated = false,
    authHeaderGen = twAuthHeaderGen)

  "Twitter request" should "properly fetch a user's timeline" in {

    val authHeader = twAuthHeaderGen(request)

    val authRequest = request.copy(
      headers = request.headers ++ Seq(authHeader))

    val conn = new PlayWSHttpConnection
    val respF = conn.makeRequest(authRequest)
    val resp = Await.result(respF, 10.seconds)
    assert(resp.status.equals(200))
    assert(resp.json.toString().contains("created_at"))
  }
}
