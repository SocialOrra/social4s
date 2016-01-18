package facebook4s

import play.api.GlobalSettings
import play.api.libs.json.Json
import play.api.test._
import play.api.mvc._
import play.api.mvc.BodyParsers._
import play.api.mvc.Results._

import scala.collection.immutable.TreeMap
import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.Future

import com.typesafe.config.ConfigFactory
import org.scalatest.BeforeAndAfterAll

import org.scalatestplus.play._

import scala.concurrent.duration._
import scala.concurrent.Await

class FacebookRequestBuilderSpec extends PlaySpec with OneServerPerSuite with BeforeAndAfterAll {

  import FacebookConnection._
  import FacebookTestHelpers._

  val config = ConfigFactory.load("test.conf")

  implicit lazy val cfg: FacebookConnectionInformation = FacebookConnectionInformation()
  implicit lazy val conn = new FacebookConnection

  val accessToken = AccessToken(config.getString("facebook4s.test.access-token"), 0L)
  val adId = config.getString("facebook4s.test.ad-id")
  val profileName = config.getString("facebook4s.test.profile-name")
  implicit val accessTokenOpt = Some(accessToken)

  val multipartAction = Action.async(parse.multipartFormData) { request ⇒
    Future {

      val limit = 30

      val data = TreeMap(
        (0 until 1000) map { n ⇒
          n -> Json.obj("key" -> n, "value" -> s"value-$n")
        }: _*)

      val batchPart = request.body.dataParts.filterKeys(_ == "batch").headOption

      val json = batchPart.flatMap(_._2.headOption).map(Json.parse)
      val jsonResponse = makeJsonDataResponse(NUM_SUCCESSES)

      json.map { j ⇒
        val urls = j \\ "relative_url"
        val sinceUntilSeq = urls.map(_.as[String].split("\\?").tail.head.split("&").filter(s ⇒ s.startsWith("since=") || s.startsWith("until=")).map(kv ⇒ kv.split("=")))
        sinceUntilSeq.map { sinceUntil ⇒
          val since = sinceUntil(0)(1).toLong
          val until = sinceUntil(1)(1).toLong
          // - if since / until not satisfied, return subset, compute next / prev links
          println(s"since=$since, until=$until")
        }
      }

      println("jsonRequest=" + json.get.toString)
      println("jsonResponse=" + jsonResponse)

      Ok(jsonResponse)
    }
  }

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
    val requestBuilder = FacebookRequestBuilder()

    requestBuilder.get("me", Map.empty, since = None, until = None, accessToken = None)
    val future = requestBuilder.execute
    val response = Await.result(future, 5.seconds)

    assert(response.parts.size == 1)
    assert((response.parts.head.bodyJson \ "name").validate[String].get == profileName)
  }

  "Paginate requests" in {

    import facebook4s.FacebookMarketingApi._

    implicit lazy val cfg: FacebookConnectionInformation = FacebookConnectionInformation(
      graphApiHost = s"localhost:$port",
      protocol = "http")

    implicit lazy val conn = new FacebookConnection
    val requestBuilder = FacebookRequestBuilder()
    requestBuilder.adInsights("123", since = Some(0), until = Some(100))
    requestBuilder.adInsights("123", since = Some(0), until = Some(100))
    val future = requestBuilder.executeWithPagination
    val response = Await.result(future, 10.seconds)
    println("response=" + response)

    conn.shutdown()
  }

  override def afterAll(): Unit = {
    conn.shutdown()
  }
}
