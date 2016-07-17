import akka.actor.ActorSystem

import com.typesafe.config.ConfigFactory
import http.client.connection.impl.{PlayWSHttpConnection, ThrottledHttpConnection}
import http.client.method.GetMethod
import http.client.response._
import http.client.request._
import twitter4s.request._
import twitter4s.api._
import twitter4s.api.TwitterApi._

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

val config = ConfigFactory.load()
val oauthConsumerSecret = config.getString("twitter4s.console.oauth-consumer-secret")
val oauthConsumerKey = config.getString("twitter4s.console.oauth-consumer-key")
val oauthToken = config.getString("twitter4s.console.oauth-token")
val oauthTokenSecret = config.getString("twitter4s.console.oauth-token-secret")

implicit val twAuthHeaderGen = TwitterAuthorizationHeader.generate(
  oauthConsumerKey = oauthConsumerKey,
  oauthToken = oauthToken,
  oauthConsumerSecret = oauthConsumerSecret,
  oauthTokenSecret = oauthTokenSecret)(_)

val conn = new ThrottledHttpConnection {
  override val actorSystem = ActorSystem("twitter4s-console")
  override val connection = new PlayWSHttpConnection
}

val requestBuilder = new TwitterRequestBuilder(conn)

implicit val acc = new TwitterBatchRequestAccumulatorCallback
