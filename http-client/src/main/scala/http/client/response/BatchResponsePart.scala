package http.client.response

import play.api.libs.json.JsValue

trait BatchResponsePart {
  val code: Int
  val headers: Seq[BatchResponsePartHeader]
  val body: String
  val bodyJson: JsValue
}
