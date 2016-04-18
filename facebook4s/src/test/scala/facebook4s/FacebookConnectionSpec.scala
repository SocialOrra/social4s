package facebook4s

import facebook4s.api.AccessToken
import facebook4s.connection.{WSClient, FacebookConnectionInformation}
import facebook4s.request.FacebookBatchRequestBuilder
import facebook4s.response.FacebookBatchResponsePart
import org.scalatest.BeforeAndAfterAll
import org.scalatestplus.play._

import play.api.GlobalSettings
import play.api.libs.json.Json
import play.api.test._
import play.api.mvc._
import play.api.mvc.BodyParsers._
import play.api.mvc.Results._

import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.Future

class FacebookConnectionSpec extends PlaySpec with OneServerPerSuite with BeforeAndAfterAll {

  import FacebookTestHelpers._

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

  "Parse batch responses" in {

    val jsonResponse = makeJsonResponse(NUM_SUCCESSES, NUM_ERRORS)
    val parts = Json.parse(jsonResponse).validate[Seq[FacebookBatchResponsePart]].getOrElse(Seq.empty)

    val returnCodes = (1 to NUM_SUCCESSES).map { _ ⇒ HTTP_SUCCESS_CODE } ++ (1 to NUM_ERRORS).map { _ ⇒ HTTP_ERROR_CODE }
    assert(parts.size == NUM_SUCCESSES + NUM_ERRORS)
    assert(parts.map(_.status) == returnCodes)

    parts.take(NUM_SUCCESSES).map(_.json) foreach { json ⇒
      (json \ "name").validate[String].get == NAME
    }
    parts.takeRight(NUM_ERRORS).map(_.json) foreach { json ⇒
      (json \ "error" \ "code").validate[Int].get == JSON_ERROR_CODE
    }

  }
}
