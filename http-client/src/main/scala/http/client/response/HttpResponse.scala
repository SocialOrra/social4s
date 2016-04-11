package http.client.response

import play.api.libs.json.JsValue

trait HttpResponse {
  val status: Int
  val headers: Map[String, Seq[String]]
  def statusText: String
  def body: String
  def bodyAsBytes: Array[Byte]
  def json: JsValue
}
