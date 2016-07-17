package twitter4s

import akka.actor.ActorSystem

import com.typesafe.config.ConfigFactory
import http.client.connection.impl.{PlayWSHttpConnection, ThrottledHttpConnection}
import http.client.method.GetMethod
import http.client.request.HttpRequestAccumulatorCallback
import http.client.response.HttpHeader
import org.scalatest._
import twitter4s.request._

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

class TwitterRequestBuilderSpec extends FlatSpec with Matchers with OptionValues with Inside with Inspectors {

  val config = ConfigFactory.load("test.conf")
  val _baseUrl = config.getString("twitter4s.test.base-url")
  val _relativeUrl = config.getString("twitter4s.test.relative-url")
  val _headers = Seq.empty[HttpHeader]
  val _queryString = Map("screen_name" → Seq("codewarrior"), "max_id" → Seq("848502337"))

  val oauthConsumerSecret = config.getString("twitter4s.test.oauth-consumer-secret")
  val oauthConsumerKey = config.getString("twitter4s.test.oauth-consumer-key")
  val oauthToken = config.getString("twitter4s.test.oauth-token")
  val oauthTokenSecret = config.getString("twitter4s.test.oauth-token-secret")

  implicit val twAuthHeaderGen = TwitterAuthorizationHeader.generate(
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
    paginated = true,
    authHeaderGen = twAuthHeaderGen)

  "TwitterRequestBuilder" should "properly fetch a user's timeline" in {

    val authHeader = twAuthHeaderGen(request)

    val authRequest = request.copy(
      headers = _headers ++ Seq(authHeader))

    val conn = new ThrottledHttpConnection {
      override val actorSystem = ActorSystem("twitter4s-test")
      override val connection = new PlayWSHttpConnection
    }
    val requestBuilder = new TwitterRequestBuilder(conn)
    val acc = new HttpRequestAccumulatorCallback
    val respF = requestBuilder.makeRequest(authRequest, acc)
    val resp = Await.result(respF, 30.seconds)
    assert(resp)
    assert(acc.completedRequests.forall(_.status == 200))
    assert(acc.completedRequests.head.json.toString().contains("created_at"))
    assert(acc.completedRequests.size > 1)
  }

  "TwitterRequestBuilder" should "properly fetch a user's followers and follow cursors" in {

    val _relativeUrl = "/1.1/followers/ids.json"
    val _queryString = Map("screen_name" → Seq("theSeanCook"), "count" → Seq("5000"))

    val request = TwitterCursoredRequest(
      baseUrl = _baseUrl,
      relativeUrl = _relativeUrl,
      headers = _headers,
      method = GetMethod,
      queryString = _queryString,
      body = None,
      paginated = true,
      authHeaderGen = twAuthHeaderGen)

    val authHeader = twAuthHeaderGen(request)

    val authRequest = request.copy(
      headers = _headers ++ Seq(authHeader))

    val conn = new ThrottledHttpConnection {
      override val actorSystem = ActorSystem("twitter4s-test")
      override val connection = new PlayWSHttpConnection
    }

    val requestBuilder = new TwitterRequestBuilder(conn)
    val acc = new HttpRequestAccumulatorCallback
    val respF = requestBuilder.makeRequest(authRequest, acc)
    val resp = Await.result(respF, 30.seconds)
    assert(resp)
    assert(acc.completedRequests.forall(_.status == 200))
    assert(acc.completedRequests.size > 1)
  }
}
