package facebook4s.response

import http.client.response.{BatchResponse, HttpHeader}
import play.api.libs.json.Json

case class FacebookBatchResponse(override val code: Int, override val headers: Seq[HttpHeader], override val parts: Seq[FacebookBatchResponsePart])
  extends BatchResponse[FacebookBatchResponsePart]

object FacebookBatchResponse {

  implicit val facebookBatchResponsePartFmt = Json.format[FacebookBatchResponsePart]

}

