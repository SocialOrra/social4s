package facebook4s

import play.api.libs.json.Json
import play.api.libs.ws.WSResponse

case class FacebookBatchResponse(code: Int, headers: Map[String, Seq[String]], parts: Seq[FacebookBatchResponsePart])

object FacebookBatchResponse {

  object Implicits {
    implicit val facebookBatchResponsePartHeaderFmt = Json.format[FacebookBatchResponsePartHeader]
    implicit val facebookBatchResponsePartFmt = Json.format[FacebookBatchResponsePart]
  }

  import Implicits._

  def apply(wsResponse: WSResponse): FacebookBatchResponse = {
    FacebookBatchResponse(wsResponse.status, wsResponse.allHeaders, wsResponse.json.validate[Seq[FacebookBatchResponsePart]].getOrElse(Seq.empty))
  }
}

