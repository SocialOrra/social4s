package facebook4s.response

import http.client.response.{BatchResponse, HttpHeader}
import play.api.libs.json.JsValue

case class FacebookBatchResponse(
  override val status:      Int,
  override val statusText:  String,
  override val headers:     Seq[HttpHeader],
  override val bodyAsBytes: Array[Byte],
  override val json:        JsValue,
  override val parts:       Seq[FacebookBatchResponsePart])
    extends BatchResponse[FacebookBatchResponsePart] {
  override val body: String = new String(bodyAsBytes)
}

