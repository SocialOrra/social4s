package twitter4s

import akka.actor.ActorSystem

import com.typesafe.config.ConfigFactory
import http.client.connection.impl.{PlayWSHttpConnection, ThrottledHttpConnection}
import http.client.method.GetMethod
import http.client.response.HttpHeader
import org.scalatest._
import twitter4s.request.{TwitterAuthorizationHeader, TwitterCursoredRequest, TwitterRequestBuilder, TwitterTimelineRequest}

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

class TwitterRequestBuilderSpec extends FlatSpec with Matchers with OptionValues with Inside with Inspectors {

  val config = ConfigFactory.load("test.conf")
  val _baseUrl = config.getString("twitter4s.test.base-url")
  val _relativeUrl = config.getString("twitter4s.test.relative-url")
  val _headers = Seq.empty[HttpHeader]
  val _queryString = Map("screen_name" → Seq("codewarrior"), "max_id" → Seq("848502337"))

  val request = TwitterTimelineRequest(
    baseUrl = _baseUrl,
    relativeUrl = _relativeUrl,
    headers = _headers,
    method = GetMethod,
    queryString = _queryString,
    body = None,
    paginated = true)

  val oauthConsumerSecret = config.getString("twitter4s.test.oauth-consumer-secret")
  val oauthConsumerKey = config.getString("twitter4s.test.oauth-consumer-key")
  val oauthToken = config.getString("twitter4s.test.oauth-token")
  val oauthTokenSecret = config.getString("twitter4s.test.oauth-token-secret")

  implicit val twAuthHeaderGen = TwitterAuthorizationHeader.generate(
    oauthConsumerKey = oauthConsumerKey,
    oauthToken = oauthToken,
    oauthConsumerSecret = oauthConsumerSecret,
    oauthTokenSecret = oauthTokenSecret)(_)

  "TwitterRequestBuilder" should "properly fetch a user's timeline" in {

    val authHeader = twAuthHeaderGen(request)

    val authRequest = request.copy(
      headers = _headers ++ Seq(authHeader))

    val conn = new ThrottledHttpConnection {
      override val actorSystem = ActorSystem("twitter4s-test")
      override val connection = new PlayWSHttpConnection
    }
    val requestBuilder = new TwitterRequestBuilder(conn)
    val respF = requestBuilder.makeRequest(authRequest)
    val resp = Await.result(respF, 30.seconds)
    assert(resp._2.forall(_.status.equals(200)))
    assert(resp._2.head.json.toString().contains("created_at"))
    assert(resp._2.size > 1)
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
      paginated = true)

    val authHeader = twAuthHeaderGen(request)

    val authRequest = request.copy(
      headers = _headers ++ Seq(authHeader))

    val conn = new ThrottledHttpConnection {
      override val actorSystem = ActorSystem("twitter4s-test")
      override val connection = new PlayWSHttpConnection
    }

    val requestBuilder = new TwitterRequestBuilder(conn)
    val respF = requestBuilder.makeRequest(authRequest)
    val resp = Await.result(respF, 30.seconds)
    assert(resp._2.forall(_.status.equals(200)))
    assert(resp._2.size > 1)
  }
}
