package facebook4s.response

import play.api.libs.json.Json
import play.api.libs.ws.WSResponse

case class FacebookBatchResponse(code: Int, headers: Map[String, Seq[String]], parts: Seq[FacebookBatchResponsePart])

object FacebookBatchResponse {

  implicit val facebookBatchResponsePartFmt = Json.format[FacebookBatchResponsePart]

  def fromWSResponse(wsResponse: WSResponse): FacebookBatchResponse = {
    FacebookBatchResponse(wsResponse.status, wsResponse.allHeaders, wsResponse.json.validate[Seq[FacebookBatchResponsePart]].getOrElse(Seq.empty))
  }
}

