package facebook4s

import com.typesafe.config.ConfigFactory
import org.scalatest.BeforeAndAfterAll

import org.scalatestplus.play._

import scala.concurrent.duration._
import scala.concurrent.Await

import scala.concurrent.ExecutionContext.Implicits._

class FacebookRequestBuilderSpec extends PlaySpec with BeforeAndAfterAll {

  import FacebookConnection._

  val config = ConfigFactory.load("test.conf")

  implicit lazy val cfg: FacebookConnectionInformation = FacebookConnectionInformation()
  implicit val conn = new FacebookConnection

  val accessToken = AccessToken(config.getString("facebook4s.test.access-token"), 0L)
  implicit val accessTokenOpt = Some(accessToken)
  val adId = config.getString("facebook4s.test.ad-id")
  val profileName = config.getString("facebook4s.test.profile-name")

  "Successfully use the Facebook Graph API" in {
    val requestBuilder = FacebookRequestBuilder()

    requestBuilder.get("me", Map.empty, None)
    val future = requestBuilder.execute
    val responseRaw = Await.result(future, 5.seconds)
    val response = FacebookBatchResponse(responseRaw)

    assert(response.parts.size == 1)
    assert((response.parts.head.bodyJson \ "name").validate[String].get == profileName)
  }

  override def afterAll(): Unit = {
    conn.shutdown()
  }
}
