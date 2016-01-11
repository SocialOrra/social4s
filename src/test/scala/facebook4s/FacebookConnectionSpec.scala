package facebook4s

import org.scalatestplus.play._

import play.api.GlobalSettings
import play.api.test.Helpers._
import play.api.test._
import play.api.mvc._
import play.api.mvc.BodyParsers._
import play.api.mvc.Results._

import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.Future

class FacebookConnectionSpec extends PlaySpec with OneServerPerSuite {

  import FacebookConnection._

  val jsonAction = Action.async(parse.json) { request ⇒ Future { Ok(request.body) } }
  val echoBodyAction = Action.async(parse.text) { request ⇒ Future { Ok(request.body) } }

  implicit override lazy val app: FakeApplication =
    FakeApplication(
      withGlobal = Some(new GlobalSettings() {
        override def onRouteRequest(request: RequestHeader): Option[Handler] = {
          request.uri match {
            case uri if uri.startsWith("/json") ⇒ Some(jsonAction)
            case uri if uri.startsWith("/v")    ⇒ Some(echoBodyAction)
            case uri                            ⇒ super.onRouteRequest(request)
          }
        }
      }))

  implicit lazy val cfg: FacebookConnectionInformation = FacebookConnectionInformation(
    graphApiHost = s"localhost:$port",
    protocol = "http")

  "Properly construct GET requests" in {
    implicit val accessToken = AccessToken("abc", 0L)
    val qs = Map("f1" -> Seq("v1"), "f2" -> Seq("v2"))
    val request = buildGet("me", qs)
    val requestUrl = url(cfg.protocol, cfg.graphApiHost, cfg.version, "me", qs, accessToken)
    assert(request.uri.toString == requestUrl)
  }

  "Properly construct POST reuqests" in {
    implicit val accessToken = AccessToken("abc", 0L)
    val qs = Map("f1" -> Seq("v1"), "f2" -> Seq("v2"))
    val request = buildPost("me", "post-body", qs)
    val requestUrl = url(cfg.protocol, cfg.graphApiHost, cfg.version, "me", qs, accessToken)
    val response = await(request.execute())
    assert(request.uri.toString == requestUrl)
    assert(response.body == "post-body")
  }

  private def url(scheme: String, host: String, version: String, relativeUrl: String, queryString: Map[String, Seq[String]], accessToken: AccessToken): String = {
    val qs = FacebookConnection.queryStringToSeq(queryString) :+ FacebookConnection.accessTokenQS(accessToken)
    val qsString = qs.map { case (key, value) ⇒ key + "=" + value }.mkString("&")
    s"$scheme://$host/v$version/$relativeUrl?$qsString"
  }
}
