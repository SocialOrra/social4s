package facebook4s.response

import play.api.libs.json.Json

/** Represents time based paging information within a Facebook response.
 *  (previous and next are unix time stamps)
 */
case class FacebookTimePaging(previous: String, next: String) {

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

object FacebookTimePaging {
  implicit val pagingFmt = Json.format[FacebookTimePaging]

  def fromLongs(previousSince: Long, previousUntil: Long, nextSince: Long, nextUntil: Long) = {
    FacebookTimePaging(s"since=$previousSince&until=$previousUntil", s"since=$nextSince&until=$nextUntil")
  }
}

