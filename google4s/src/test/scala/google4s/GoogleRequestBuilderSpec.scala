package google4s

import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory
import google4s.request.{GoogleAccessToken, GoogleRequest, GoogleRequestBuilder}
import http.client.connection.impl.{PlayWSHttpConnection, ThrottledHttpConnection}
import http.client.method.GetMethod
import org.scalatest._

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

class GoogleRequestBuilderSpec extends FlatSpec with Matchers with OptionValues with Inside with Inspectors with BeforeAndAfterAll {

  val config = ConfigFactory.load("test.conf")

  val _refreshToken = config.getString("google4s.test.refresh-token")
  val _clientId = config.getString("google4s.test.client-id")
  val _clientSecret = config.getString("google4s.test.client-secret")

  val _actorSystem = ActorSystem("google4s-test")

  val conn = new ThrottledHttpConnection {
    override val actorSystem = _actorSystem
    override val connection = new PlayWSHttpConnection
  }

  val _accessToken = {

    val conn = new ThrottledHttpConnection {
      override val actorSystem = _actorSystem
      override val connection = new PlayWSHttpConnection
    }

    val requestBuilder = new GoogleRequestBuilder(conn)

    val accessTokenF = GoogleAccessToken
      .fromRenewToken(requestBuilder)(_clientSecret, _clientId, _refreshToken, global)
      .map { t ⇒
        t.getOrElse {
          println("No access token could be retrieved, expect failures.")
          "NO_ACCESS_TOKEN"
        }
      }

    val a = Await.result(accessTokenF, 10.seconds)
    requestBuilder.shutdown()
    a
  }

  override def afterAll(): Unit = {
    conn.shutdown()
    _actorSystem.shutdown()
  }

  "GoogleRequestBuilder" should "properly make non-paginated requests" in {

    val request = new GoogleRequest(
      baseUrl = "https://www.googleapis.com/analytics/v3",
      relativeUrl = "/data/ga",
      method = GetMethod,
      accessToken = _accessToken,
      paginated = false,
      _queryString = Map(
        "ids" → Seq("ga:84943435"),
        "start-date" → Seq("2015-12-01"),
        "end-date" → Seq("2016-01-01"),
        "metrics" → Seq("ga:sessions"),
        "dimensions" → Seq("ga:socialNetwork"),
        "max-results" → Seq("1")))

    val requestBuilder = new GoogleRequestBuilder(conn)
    val responseF = requestBuilder.makeRequest(request)
    val response = Await.result(responseF, 10.seconds)

    assert(response._2.head.status.equals(200))
    assert(response._2.size == 1)
  }

  it should "properly paginate through requests until the end" in {

    val request = new GoogleRequest(
      baseUrl = "https://www.googleapis.com/analytics/v3",
      relativeUrl = "/data/ga",
      method = GetMethod,
      accessToken = _accessToken,
      paginated = true,
      _queryString = Map(
        "ids" → Seq("ga:84943435"),
        "start-date" → Seq("2015-12-01"),
        "end-date" → Seq("2016-01-01"),
        "metrics" → Seq("ga:sessions"),
        "dimensions" → Seq("ga:socialNetwork"),
        "max-results" → Seq("1")))

    val requestBuilder = new GoogleRequestBuilder(conn)
    val responseF = requestBuilder.makeRequest(request)
    val response = Await.result(responseF, 10.seconds)

    assert(response._2.head.status.equals(200))
    assert(response._2.size > 1)
  }

  it should "refresh expired access tokens via a refresh token" in {

    val requestBuilder = new GoogleRequestBuilder(conn)
    val responseF = GoogleAccessToken.fromRenewToken(requestBuilder)(_clientSecret, _clientId, _refreshToken, global)
    val response = Await.result(responseF, 10.seconds)

    assert(response.isDefined)
  }
}
