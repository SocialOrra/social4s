package twitter4s

import com.typesafe.config.ConfigFactory
import http.client.method.GetMethod
import http.client.response.HttpHeader
import org.scalatest._

class TwitterAuthorizationHeaderGetRequestSpec extends FlatSpec with Matchers with OptionValues with Inside with Inspectors {

  val config = ConfigFactory.load("test.conf")

  val _baseUrl = config.getString("twitter4s.test.base-url")
  val _method = GetMethod
  val _relativeUrl = config.getString("twitter4s.test.relative-url")
  val _headers = Seq.empty[HttpHeader]
  // TODO: fetch from config
  val _queryString = Map("screen_name" → Seq("codewarrior"))

  val request = TwitterTimelineRequest(
    relativeUrl = _relativeUrl,
    headers = _headers,
    method = _method,
    queryString = _queryString,
    body = None,
    paginated = false
  )

  val oauthConsumerSecret = config.getString("twitter4s.test.oauth-consumer-secret")
  val oauthConsumerKey = config.getString("twitter4s.test.oauth-consumer-key")
  val oauthToken = config.getString("twitter4s.test.oauth-token")
  val oauthTokenSecret = config.getString("twitter4s.test.oauth-token-secret")
  val oauthNonce = config.getString("twitter4s.test.oauth-nonce")
  val oauthTimestamp = config.getString("twitter4s.test.oauth-timestamp")
  val expectedSignatureBaseString = config.getString("twitter4s.test.expected-signature-base-string")
  val expectedAuthHeaderName = config.getString("twitter4s.test.expected-auth-header-name")
  val expectedAuthHeaderValue = config.getString("twitter4s.test.expected-auth-header-value")

  val twAuthHeaderGen = TwitterAuthorizationHeader.generate(
    oauthConsumerKey = oauthConsumerKey,
    oauthToken = oauthToken,
    oauthConsumerSecret = oauthConsumerSecret,
    oauthTokenSecret = oauthTokenSecret,
    oauthNonce = oauthNonce,
    oauthTimestamp = oauthTimestamp
  )(_, _)

  it should "create a valid signature base string for GETs" in {
    val signatureBaseString = TwitterAuthorizationHeader.createSignatureBaseString(_baseUrl, request, _parameterString)
    assert(signatureBaseString.equals(expectedSignatureBaseString))
  }

  it should "create valid authorization headers for GETs " in {
    val authHeader = twAuthHeaderGen(_baseUrl, request)
    assert(authHeader._1.equals(expectedAuthHeaderName))
    assert(authHeader._2.equals(expectedAuthHeaderValue))
  }

  private def _parameterString = {
    val fieldsWithoutSignature = TwitterAuthorizationHeader.createOauthFieldsWithoutSignature(
      oauthConsumerKey,
      oauthToken,
      oauthConsumerSecret,
      oauthTokenSecret,
      oauthNonce,
      oauthTimestamp
    )
    TwitterAuthorizationHeader.createParameterString(request, fieldsWithoutSignature)
  }
}
