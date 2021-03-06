import akka.actor.ActorSystem

import facebook4s.api.{ AccessToken, FacebookMarketingApi }
import facebook4s.connection.FacebookConnectionInformation
import facebook4s.request.{ FacebookBatchRequestBuilder, FacebookGetRequest }
import facebook4s.response.{FacebookBatchResponsePart, FacebookTimePaging}
import facebook4s.api._
import facebook4s.api.FacebookMarketingApi
import facebook4s.api.FacebookMarketingApi._
import facebook4s.api.FacebookGraphApi
import facebook4s.api.FacebookGraphApi._

import http.client.connection.impl.{ PlayWSHttpConnection, ThrottledHttpConnection }
import http.client.request._

import play.api.libs.json.{ JsArray, Json }

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global

import com.typesafe.config.ConfigFactory

val config = ConfigFactory.load()
val accessTokenStr = config.getString("facebook4s.console.access-token")
val accessTokenOpt = Some(AccessToken(accessTokenStr, 0L))
val system = ActorSystem("facebook4s-console")

lazy val cfg: FacebookConnectionInformation = FacebookConnectionInformation()

val connection = new ThrottledHttpConnection {
  override val actorSystem = system
  override val connection = new PlayWSHttpConnection
}

val requestBuilder = new FacebookBatchRequestBuilder(cfg, connection, accessTokenOpt)

sys.addShutdownHook {
  requestBuilder.shutdown()
  system.shutdown()
}

