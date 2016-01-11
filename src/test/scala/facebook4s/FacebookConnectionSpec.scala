package facebook4s

import org.scalatest._

abstract class UnitSpec extends FlatSpec
  with Matchers
  with OptionValues
  with Inside
  with Inspectors

class FacebookConnectionSpec extends UnitSpec {

  import FacebookConnection._

  "FacebookConnection" should "property construct GET requests" in {
    implicit val accessToken = AccessToken("abc", 0L)
    val qs = Map("f1" -> Seq("v1"), "f2" -> Seq("v2"))
    val request = buildGet("me", qs)
    val requestUrl = url(PROTOCOL, FB_GRAPH_DOMAIN, FB_API_VERSION, "me", qs, accessToken)
    assert(request.uri.toString == requestUrl)
  }

  def url(scheme: String, host: String, version: String, relativeUrl: String, queryString: Map[String, Seq[String]], accessToken: AccessToken): String = {
    val qs = FacebookConnection.queryStringToSeq(queryString) :+ FacebookConnection.accessTokenQS(accessToken)
    val qsString = qs.map { case (key, value) ⇒ key + "=" + value }.mkString("&")
    s"$scheme://$host/v$version/$relativeUrl?$qsString"
  }
}
