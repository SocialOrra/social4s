package http.client.response

import play.api.libs.json.Json

case class HttpHeader(name: String, value: String)

object HttpHeader {
  implicit val fmt = Json.format[HttpHeader]
}
