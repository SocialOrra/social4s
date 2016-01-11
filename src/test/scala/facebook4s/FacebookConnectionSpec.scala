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
  val multipartAction = Action.async(parse.multipartFormData) { request ⇒ Future { Ok(request.body.dataParts.toSeq.sortBy(_._1).map(d ⇒ d._1 + d._2.mkString).mkString) } }

  implicit override lazy val app: FakeApplication =
    FakeApplication(
      withGlobal = Some(new GlobalSettings() {
        override def onRouteRequest(request: RequestHeader): Option[Handler] = {
          request.uri match {
            case uri if uri.startsWith("/json") ⇒ Some(jsonAction)
            case uri if uri.endsWith("/v2.5/")  ⇒ Some(multipartAction)
            case uri if uri.startsWith("/v")    ⇒ Some(echoBodyAction)
            case uri                            ⇒ super.onRouteRequest(request)
          }
        }
      }))

  implicit lazy val cfg: FacebookConnectionInformation = FacebookConnectionInformation(
    graphApiHost = s"localhost:$port",
    protocol = "http")

  implicit val accessToken = AccessToken("abc", 0L)

  "Properly construct GET requests" in {
    val qs = Map("f1" -> Seq("v1"), "f2" -> Seq("v2"))
    val request = buildGet("me", qs)
    val requestUrl = url(cfg.protocol, cfg.graphApiHost, cfg.version, "me", qs, Some(accessToken))
    assert(request.uri.toString == requestUrl)
  }

  "Properly construct POST requests" in {
    val qs = Map("f1" -> Seq("v1"), "f2" -> Seq("v2"))
    val request = buildPost("me", "post-body", qs)
    val requestUrl = url(cfg.protocol, cfg.graphApiHost, cfg.version, "me", qs, Some(accessToken))
    val response = await(request.execute())
    assert(request.uri.toString == requestUrl)
    assert(response.body == "post-body")
  }

  "Properly construct batch requests" in {
    val parts = Seq(
      "a" -> "a".getBytes("utf-8"),
      "b" -> "b".getBytes("utf-8"),
      "c" -> "c".getBytes("utf-8"))

    val request = buildBatch(parts)
    val requestUrl = url(cfg.protocol, cfg.graphApiHost, cfg.version, "", Map.empty, None)
    val response = await(request.execute())
    assert(request.uri.toString == requestUrl)
    assert(response.body == "aabbcc")
  }

  private def url(scheme: String, host: String, version: String, relativeUrl: String, queryString: Map[String, Seq[String]], accessToken: Option[AccessToken]): String = {
    val qs = FacebookConnection.queryStringToSeq(queryString) ++ accessToken.map(a ⇒ Seq(FacebookConnection.accessTokenQS(a))).getOrElse(Seq.empty)

    val qsString = {
      val q = qs.map { kv ⇒ kv._1 + "=" + kv._2 }.mkString("&")
      if (q.nonEmpty) "?" + q
      else q
    }

    s"$scheme://$host/$version/$relativeUrl$qsString"
  }
}
