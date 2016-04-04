package http.client.response

import play.api.libs.json.JsValue

trait HttpResponse {
  val status: Int
  val headers: Map[String, Seq[String]]
  def json: JsValue
}
