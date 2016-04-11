package twitter4s

import http.client.method.PostMethod
import http.client.request.PostRequest
import org.scalatest._

class TwitterAuthorizationHeaderPostRequestSpec extends FlatSpec with Matchers with OptionValues with Inside with Inspectors {

  val baseUrl = "https://api.twitter.com"
  val method = PostMethod
  val relativeUrl = "/1/statuses/update.json"
  val headers = Seq(
    "Accept" -> "*/*",
    "Connection" -> "close",
    "User-Agent" -> "OAuth gem v0.4.4",
    "Content-Type" -> "application/x-www-form-urlencoded",
    "Content-Length" -> "76",
    "Host" -> "api.twitter.com")

  val queryString = Map("include_entities" -> Seq("true"))
  val body = "status=Hello Ladies + Gentlemen, a signed OAuth request!"

  val request = new PostRequest[Array[Byte]](
    relativeUrl = relativeUrl,
    headers = headers,
    queryString = queryString,
    body = Some(body.getBytes("utf-8")),
    method = method) {
    override def toJson(extraQueryStringParams: Map[String, Seq[String]]): String = ""
  }

  val oauthConsumerSecret = "kAcSOqF21Fu85e7zjz7ZN2U4ZRhfV3WpwPAoE3Z7kBw"
  val oauthConsumerKey = "xvz1evFS4wEEPTGEFPHBog"
  val oauthToken = "370773112-GmHxMAgYyLbNEtIKZeRNFsMKPR9EyMZeS9weJAEb"
  val oauthTokenSecret = "LswwdoUaIvS8ltyTt5jkRh4J50vUPVVHtR2YPi5kE"
  val oauthTimestamp = "1318622958"
  val oauthNonce = "kYjzVBB8Y0ZFabxSWbWovY3uYSQ2pTgmZeNu2VS4cg"

  val expectedSigningKey = "kAcSOqF21Fu85e7zjz7ZN2U4ZRhfV3WpwPAoE3Z7kBw&LswwdoUaIvS8ltyTt5jkRh4J50vUPVVHtR2YPi5kE"
  val expectedParameterString = "include_entities=true&oauth_consumer_key=xvz1evFS4wEEPTGEFPHBog&oauth_nonce=kYjzVBB8Y0ZFabxSWbWovY3uYSQ2pTgmZeNu2VS4cg&oauth_signature_method=HMAC-SHA1&oauth_timestamp=1318622958&oauth_token=370773112-GmHxMAgYyLbNEtIKZeRNFsMKPR9EyMZeS9weJAEb&oauth_version=1.0&status=Hello%20Ladies%20%2B%20Gentlemen%2C%20a%20signed%20OAuth%20request%21"
  val expectedSignatureBaseString = "POST&https%3A%2F%2Fapi.twitter.com%2F1%2Fstatuses%2Fupdate.json&include_entities%3Dtrue%26oauth_consumer_key%3Dxvz1evFS4wEEPTGEFPHBog%26oauth_nonce%3DkYjzVBB8Y0ZFabxSWbWovY3uYSQ2pTgmZeNu2VS4cg%26oauth_signature_method%3DHMAC-SHA1%26oauth_timestamp%3D1318622958%26oauth_token%3D370773112-GmHxMAgYyLbNEtIKZeRNFsMKPR9EyMZeS9weJAEb%26oauth_version%3D1.0%26status%3DHello%2520Ladies%2520%252B%2520Gentlemen%252C%2520a%2520signed%2520OAuth%2520request%2521"
  val expectedAuthHeaderValue = """OAuth oauth_consumer_key="xvz1evFS4wEEPTGEFPHBog", oauth_nonce="kYjzVBB8Y0ZFabxSWbWovY3uYSQ2pTgmZeNu2VS4cg", oauth_signature="tnnArxj06cWHq44gCs1OSKk%2FjLY%3D", oauth_signature_method="HMAC-SHA1", oauth_timestamp="1318622958", oauth_token="370773112-GmHxMAgYyLbNEtIKZeRNFsMKPR9EyMZeS9weJAEb", oauth_version="1.0""""
  val expectedAuthHeaderName = "Authorization"

  val twAuthHeaderGen = TwitterAuthorizationHeader.generate(
    oauthConsumerKey = oauthConsumerKey,
    oauthToken = oauthToken,
    oauthConsumerSecret = oauthConsumerSecret,
    oauthTokenSecret = oauthTokenSecret,
    oauthNonce = oauthNonce,
    oauthTimestamp = oauthTimestamp)(_, _)

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

  "Twitter Auth Header" should "create a valid parameter string for POSTs" in {
    val parameterString = _parameterString
    assert(parameterString.equals(expectedParameterString))
  }

  it should "create a valid signature base string for POSTs" in {
    val signatureBaseString = TwitterAuthorizationHeader.createSignatureBaseString(baseUrl, request, _parameterString)
    assert(signatureBaseString.equals(expectedSignatureBaseString))
  }

  it should "create valid signing keys for POSTs" in {
    val mySigningKey = TwitterAuthorizationHeader.createSigningKey(oauthConsumerSecret, oauthTokenSecret)
    assert(mySigningKey.equals(expectedSigningKey))
  }

  it should "create valid authorization headers for POSTs " in {
    val authHeader = twAuthHeaderGen(baseUrl, request)
    assert(authHeader._1.equals(expectedAuthHeaderName))
    assert(authHeader._2.equals(expectedAuthHeaderValue))
  }
}
