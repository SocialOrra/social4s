package facebook4s

import akka.actor.ActorSystem
import facebook4s.api.AccessToken
import facebook4s.api.FacebookMarketingApi._
import facebook4s.connection.FacebookConnectionInformation
import facebook4s.request.{FacebookBatchRequestBuilder, FacebookGetRequest}
import facebook4s.response.{FacebookBatchResponsePart, FacebookTimePaging}
import http.client.connection.impl.{PlayWSHttpConnection, ThrottledHttpConnection}
import play.api.GlobalSettings
import play.api.libs.json.{JsArray, Json}
import play.api.test._
import play.api.mvc._
import play.api.mvc.BodyParsers._
import play.api.mvc.Results._

import scala.collection.immutable.TreeMap
import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.Future
import com.typesafe.config.ConfigFactory
import http.client.request.HttpBatchRequestAccumulatorCallback
import org.scalatest.BeforeAndAfterAll
import org.scalatestplus.play._

import scala.concurrent.duration._
import scala.concurrent.Await

class FacebookRequestBuilderSpec extends PlaySpec with OneServerPerSuite with BeforeAndAfterAll {

  import FacebookBatchRequestBuilder._
  import FacebookTestHelpers._

  val config = ConfigFactory.load("test.conf")

  val accessToken = AccessToken(config.getString("facebook4s.test.access-token"), 0L)
  val adId = config.getString("facebook4s.test.ad-id")
  val profileName = config.getString("facebook4s.test.profile-name")
  val accessTokenOpt = Some(accessToken)

  val limit = 30

  val data = TreeMap(
    (0 until 1000) map { n ⇒
      n → Json.obj("key" → n, "value" → s"value-$n")
    }: _*)

  val multipartAction = Action.async(parse.multipartFormData) { request ⇒
    Future {

      val batchPart = request.body.dataParts.filterKeys(_ == "batch").headOption

      val batchPartAsJsonOpt = batchPart.flatMap(_._2.headOption).map(Json.parse)

      val response = batchPartAsJsonOpt.map { batchPartAsJson ⇒

        val relativeUrls = batchPartAsJson \\ "relative_url"

        val sinceUntilSeq = relativeUrls
          .map(_.as[String]
            .split("\\?")
            .tail
            .head
            .split("&")
            .filter(s ⇒ s.startsWith("since=") || s.startsWith("until="))
            .map(_.split("=")))

        val parts = sinceUntilSeq.map { su ⇒
          val since = su(0)(1).toLong
          val until = su(1)(1).toLong

          val (sinceNormalized, untilNormalized) = if (since + until > limit) {
            // asked for more than the limit, return limit
            (since, since + limit)
          } else {
            // asked within limits
            (since, until)
          }

          val previousSince = sinceNormalized - limit
          val previousUntil = previousSince + limit
          val nextSince = untilNormalized
          val nextUntil = nextSince + limit
          val values = (sinceNormalized until untilNormalized).map { v ⇒ Json.obj("value" → v) }

          val parts = makeBatchResponsePart(
            body = makeBatchResponsePartBody(
              data = Seq(makeBatchResponsePartBodyData(name = s"s$sinceNormalized-u$untilNormalized", value = JsArray(values))),
              paging = FacebookTimePaging.fromLongs(
                previousSince,
                previousUntil,
                nextSince,
                nextUntil)))

          parts
        }

        val response = makeBatchResponse(parts = parts)

        response
      }

      //println("--- jsonRequest=" + batchPartAsJsonOpt.get)
      //println("--- jsonResponse=" + Json.toJson(response.get.parts))

      Ok(Json.toJson(response.get.parts))
    }
  }

  val cfg: FacebookConnectionInformation = FacebookConnectionInformation(
    graphApiHost = s"localhost:$port",
    protocol = "http")

  val requests = Seq(
    ("123", Some(0L), Some(100L)),
    ("456", Some(200L), Some(600L)),
    ("789", Some(300L), Some(400L)),
    ("101", Some(10L), Some(900L)))

  implicit override lazy val app: FakeApplication =
    FakeApplication(
      withGlobal = Some(new GlobalSettings() {
        override def onRouteRequest(request: RequestHeader): Option[Handler] = {
          request.uri match {
            case uri if uri.endsWith("/v2.5/") ⇒ Some(multipartAction)
            case uri                           ⇒ super.onRouteRequest(request)
          }
        }
      }))

  "Successfully use the Facebook Graph API" in {
    lazy val cfg: FacebookConnectionInformation = FacebookConnectionInformation()
    val connection = new ThrottledHttpConnection {
      override val actorSystem = ActorSystem("facebook4s-test")
      override val connection = new PlayWSHttpConnection
    }
    val requestBuilder = new FacebookBatchRequestBuilder(cfg, connection, accessTokenOpt)

    try {
      requestBuilder.add(FacebookGetRequest("me", None, Seq.empty, Map.empty, None), since = None, until = None)
      val future = requestBuilder.execute
      println("Waiting on requestBuilder's future...")
      val response = Await.result(future, 5.seconds)
      assert(response.parts.size == 1)
      assert((response.parts.head.json \ "name").validate[String].get == profileName)
    } finally {
      requestBuilder.shutdown()
    }
  }

  "Paginate time based requests" in {

    val connection = new ThrottledHttpConnection {
      val actorSystem = ActorSystem("facebook4s-test")
      val connection = new PlayWSHttpConnection
    }

    val requestBuilder = new FacebookBatchRequestBuilder(cfg, connection, accessTokenOpt)
    requests.foreach { r ⇒ requestBuilder.adInsights(r._1, since = r._2, until = r._3) }
    val acc = new HttpBatchRequestAccumulatorCallback[FacebookBatchResponsePart]
    val future = requestBuilder.executeWithPagination(acc)
    val response = Await.result(future, 10.seconds)

    requestBuilder.shutdown()

    //println("--- returned & parsed response=" + response)
    //response.groupBy(_._1).map(_._2.map(_._2).map { part ⇒
    //  part
    //}.foreach(println))

    assert(response)

    acc.completedRequests
      .groupBy(_._1)
      .mapValues(_.map(_._2))
      .foreach { kv ⇒

        val request = kv._1
        val responseParts = kv._2

        val reqId = request.relativeUrl.split("/").head
        val reqSinceUntil = requests.find(_._1 == reqId).map(r ⇒ r._2 → r._3).get
        val data = (responseParts.last.json \ "data").validate[JsArray].get
        val values = (data.head \ "values").validate[JsArray].get
        val lastValue = (values.last \ "value").validate[Long]

        println(
          "=== request ===\n" + request + ":\n" +
            "=== end condition ===\n" +
            s"lastValue:${lastValue.get} ?>= reqSinceUntil:${reqSinceUntil._2.get}" + "\n" +
            //"=== responses ===\n" + responseParts.map(_.bodyJson).mkString("\n") + "\n" +
            "=== end ===========\n\n")

        assert(lastValue.get >= reqSinceUntil._2.get)
      }
  }

  "Paginate cursor based requests" in {

    val requestBuilder = new FacebookBatchRequestBuilder(cfg, new PlayWSHttpConnection, accessTokenOpt)
    requests.foreach { r ⇒ requestBuilder.adInsights(r._1, since = r._2, until = r._3) }
    val acc = new HttpBatchRequestAccumulatorCallback[FacebookBatchResponsePart]
    val future2 = requestBuilder.executeWithPagination(acc)
    val response2 = Await.result(future2, 10.seconds)

    requestBuilder.shutdown()

    assert(response2)

    acc.completedRequests
      .groupBy(_._1)
      .mapValues(_.map(_._2))
      .foreach { reqRes ⇒
        val request = reqRes._1
        val responseParts = reqRes._2
        val reqId = request.relativeUrl.split("/").head
        val reqSinceUntil = requests.find(_._1 == reqId).map(r ⇒ r._2 → r._3).get
        val data = (responseParts.last.json \ "data").validate[JsArray].get
        val values = (data.head \ "values").validate[JsArray].get
        val lastValue = (values.last \ "value").validate[Long]

        println(
          "=== request ===\n" + request + ":\n" +
            "=== end condition ===\n" +
            s"lastValue:${lastValue.get} ?>= reqSinceUntil:${reqSinceUntil._2.get}" + "\n" +
            //"=== responses ===\n" + responseParts.map(_.bodyJson).mkString("\n") + "\n" +
            "=== end ===========\n\n")

        assert(lastValue.get >= reqSinceUntil._2.get)
      }

  }

  override def afterAll(): Unit = {
  }
}
