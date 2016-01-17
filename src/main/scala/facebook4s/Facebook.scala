package facebook4s

import play.api.libs.json.Json
import play.api.libs.ws.ning._

private[facebook4s] object WSClient {
  implicit val client = NingWSClient()
  def shutdown() = client.close()
}

// TODO: make the value a Seq[String] to account for multi-valued headers
case class FacebookBatchResponsePartHeader(name: String, value: String)

case class FacebookBatchResponsePart(code: Int, headers: Seq[FacebookBatchResponsePartHeader], body: String) {
  lazy val bodyJson = Json.parse(body)
}

object FacebookPagingInfo {
  implicit val pagingFmt = Json.format[FacebookPagingInfo]
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
}