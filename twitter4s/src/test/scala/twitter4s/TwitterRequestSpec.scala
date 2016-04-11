package twitter4s

import com.typesafe.config.ConfigFactory
import http.client.connection.impl.PlayWSHttpConnection
import http.client.method.GetMethod
import http.client.request.GetRequest
import org.scalatest._

import scala.concurrent.duration._
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global

class TwitterRequestSpec extends FlatSpec with Matchers with OptionValues with Inside with Inspectors {

  val config = ConfigFactory.load("test.conf")
  val baseUrl = config.getString("twitter4s.test.base-url")
  val method = GetMethod
  val relativeUrl = config.getString("twitter4s.test.relative-url")
  val headers = Seq.empty[(String, String)]
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

  val twAuthHeaderGen = TwitterAuthorizationHeader.generate(
    oauthConsumerKey = oauthConsumerKey,
    oauthToken = oauthToken,
    oauthConsumerSecret = oauthConsumerSecret,
    oauthTokenSecret = oauthTokenSecret)(_, _)

  "Twitter request" should "properly fetch a user's timeline" in {

    val authHeader = twAuthHeaderGen(baseUrl, request)

    val authRequest = new GetRequest(
      relativeUrl = baseUrl + relativeUrl,
      headers = headers ++ Seq(authHeader),
      queryString = queryString,
      method = method) {
      override def toJson(extraQueryStringParams: Map[String, Seq[String]]): String = ""
    }

    val conn = new PlayWSHttpConnection
    val respF = conn.get(authRequest)
    val resp = Await.result(respF, 10.seconds)
    assert(resp.status.equals(200))
    assert(resp.json.toString().contains("created_at"))
  }
}
