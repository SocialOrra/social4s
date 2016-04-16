package twitter4s

import com.typesafe.config.ConfigFactory
import http.client.connection.impl.PlayWSHttpConnection
import http.client.method.GetMethod
import http.client.response.HttpHeader
import org.scalatest._

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

class TwitterRequestBuilderSpec extends FlatSpec with Matchers with OptionValues with Inside with Inspectors {

  val config = ConfigFactory.load("test.conf")
  val _baseUrl = config.getString("twitter4s.test.base-url")
  val _relativeUrl = config.getString("twitter4s.test.relative-url")
  val _headers = Seq.empty[HttpHeader]
  val _queryString = Map("screen_name" → Seq("codewarrior"))

  val request = TwitterPaginatedRequest(
    relativeUrl = _relativeUrl,
    headers = _headers,
    method = GetMethod,
    queryString = _queryString,
    body = None
  )

  val oauthConsumerSecret = config.getString("twitter4s.test.oauth-consumer-secret")
  val oauthConsumerKey = config.getString("twitter4s.test.oauth-consumer-key")
  val oauthToken = config.getString("twitter4s.test.oauth-token")
  val oauthTokenSecret = config.getString("twitter4s.test.oauth-token-secret")

  val twAuthHeaderGen = TwitterAuthorizationHeader.generate(
    oauthConsumerKey = oauthConsumerKey,
    oauthToken = oauthToken,
    oauthConsumerSecret = oauthConsumerSecret,
    oauthTokenSecret = oauthTokenSecret
  )(_, _)

  "TwitterRequestBuilder" should "properly fetch a user's timeline" in {

    val authHeader = HttpHeader.from(twAuthHeaderGen(_baseUrl, request))

    val authRequest = request.copy(
      headers = _headers ++ Seq(authHeader),
      relativeUrl = _baseUrl + _relativeUrl
    )

    val conn = new PlayWSHttpConnection
    val requestBuilder = new TwitterRequestBuilder(conn)
    val respF = requestBuilder.makeRequest(authRequest, paginated = false)
    val resp = Await.result(respF, 10.seconds)
    assert(resp._2.head.status.equals(200))
    assert(resp._2.head.json.toString().contains("created_at"))
  }
}
