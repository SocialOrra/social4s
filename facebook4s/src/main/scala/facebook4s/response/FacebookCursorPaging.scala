package facebook4s.response

import play.api.libs.json.Json

case class FacebookCursors(after: String, before: String)
case class FacebookCursorPaging(cursors: FacebookCursors, previous: Option[String], next: Option[String])

object FacebookCursors {
  implicit val cursorsFmt = Json.format[FacebookCursors]
}

object FacebookCursorPaging {
  implicit val cursorPagingFmt = Json.format[FacebookCursorPaging]
}

