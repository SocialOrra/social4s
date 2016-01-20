package facebook4s

import play.api.libs.json.{ JsArray, JsObject, Json }
import play.api.libs.ws.ning._

private[facebook4s] object WSClient {
  implicit val client = NingWSClient()
  def shutdown() = client.close()
}

// TODO: make the value a Seq[String] to account for multi-valued headers
case class FacebookBatchResponsePartHeader(name: String, value: String) {
  lazy val toJson: JsObject = Json.obj(
    "name" -> name,
    "value" -> value)
}

case class FacebookBatchResponsePart(code: Int, headers: Seq[FacebookBatchResponsePartHeader], body: String) {
  lazy val bodyJson = Json.parse(body)
  lazy val toJson = Json.obj(
    "code" -> code,
    "headers" -> JsArray(headers.map(_.toJson)),
    "body" -> body)
}

object FacebookPagingInfo {
  implicit val pagingFmt = Json.format[FacebookPagingInfo]

  def fromLongs(previousSince: Long, previousUntil: Long, nextSince: Long, nextUntil: Long) = {
    FacebookPagingInfo(s"since=$previousSince&until=$previousUntil", s"since=$nextSince&until=$nextUntil")
  }
}

case class FacebookPagingInfo(previous: String, next: String) {

  lazy val previousSinceLong = longValue(previous, "since")
  lazy val previousUntilLong = longValue(previous, "until")

  lazy val nextSinceLong = longValue(next, "since")
  lazy val nextUntilLong = longValue(next, "until")

  private def longValue(url: String, key: String): Option[Long] =
    url
      .split("&")
      .find(_.startsWith(s"$key="))
      .map(_.splitAt(6)._2)
      .flatMap(s ⇒ if (s.length > 0) Some(s.toLong) else None)

  def toJson: JsObject = Json.obj(
    "previous" -> previous,
    "next" -> next)
}