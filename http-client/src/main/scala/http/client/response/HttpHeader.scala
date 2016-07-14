package http.client.response

import play.api.libs.json.Json

case class HttpHeader(name: String, value: String) {
  override def toString(): String = {
    s"Header(name=$name, value=$value)"
  }
}

object HttpHeader {

  implicit val fmt = Json.format[HttpHeader]

  def from(nameValue: (String, String)): HttpHeader =
    new HttpHeader(nameValue._1, nameValue._2)
}
