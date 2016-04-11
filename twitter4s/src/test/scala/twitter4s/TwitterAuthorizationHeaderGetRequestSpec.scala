package twitter4s

import com.typesafe.config.ConfigFactory
import http.client.method.GetMethod
import http.client.request.GetRequest
import org.scalatest._

class TwitterAuthorizationHeaderGetRequestSpec extends FlatSpec with Matchers with OptionValues with Inside with Inspectors {

  val config = ConfigFactory.load("test.conf")

  val baseUrl = config.getString("twitter4s.test.base-url")
  val method = GetMethod
  val relativeUrl = config.getString("twitter4s.test.relative-url")
  val headers = Seq.empty[(String, String)]
  // TODO: fetch from config
  val queryString = Map("screen_name" -> Seq("codewarrior"))

  val request = new GetRequest(
    relativeUrl = relativeUrl,
    headers = headers,
    queryString = queryString,
    method = method) {
    override def toJson(extraQueryStringParams: Map[String, Seq[String]]): String = ""
  }

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
    oauthTimestamp = oauthTimestamp)(_, _)

  it should "create a valid signature base string for GETs" in {
    val signatureBaseString = TwitterAuthorizationHeader.createSignatureBaseString(baseUrl, request, _parameterString)
    assert(signatureBaseString.equals(expectedSignatureBaseString))
  }

  it should "create valid authorization headers for GETs " in {
    val authHeader = twAuthHeaderGen(baseUrl, request)
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
      oauthTimestamp)
    TwitterAuthorizationHeader.createParameterString(request, fieldsWithoutSignature)
  }
}
