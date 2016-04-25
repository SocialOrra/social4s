package http.client.response

import play.api.libs.json.JsValue

trait HttpResponse {
  val status: Int
  val headers: Seq[HttpHeader]
  val statusText: String
  val body: String
  val bodyAsBytes: Array[Byte]
  def json: JsValue
}
