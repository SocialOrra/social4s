package facebook4s

import play.api.libs.json.Json
import play.api.libs.ws.WSResponse

case class FacebookBatchResponse(code: Int, headers: Map[String, Seq[String]], parts: Seq[FacebookBatchResponsePart])

object FacebookBatchResponse {

  trait Implicits {
    implicit val facebookBatchResponsePartHeaderFmt = Json.format[FacebookBatchResponsePartHeader]
    implicit val facebookBatchResponsePartFmt = Json.format[FacebookBatchResponsePart]
    implicit val facebookBatchResponseFmt = Json.format[FacebookBatchResponse]
  }

  import implicits._

  def fromWSResponse(wsResponse: WSResponse): FacebookBatchResponse = {
    FacebookBatchResponse(wsResponse.status, wsResponse.allHeaders, wsResponse.json.validate[Seq[FacebookBatchResponsePart]].getOrElse(Seq.empty))
  }
}

