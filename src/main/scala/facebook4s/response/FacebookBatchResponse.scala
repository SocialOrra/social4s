package facebook4s.response

import http.client.response.BatchResponse
import play.api.libs.json.Json

case class FacebookBatchResponse(override val code: Int, override val headers: Map[String, Seq[String]], override val parts: Seq[FacebookBatchResponsePart])
  extends BatchResponse

object FacebookBatchResponse {

  implicit val facebookBatchResponsePartFmt = Json.format[FacebookBatchResponsePart]

}

