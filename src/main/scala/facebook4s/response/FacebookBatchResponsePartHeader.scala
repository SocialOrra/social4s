package facebook4s.response

import play.api.libs.json.{ JsObject, Json }

/** Represents a batch response header with a name and value.
 */
case class FacebookBatchResponsePartHeader(name: String, value: String) {
  lazy val toJson: JsObject = Json.obj(
    "name" -> name,
    "value" -> value)
}

object FacebookBatchResponsePartHeader {
  implicit val batchResponsePartHeaderFmt = Json.format[FacebookBatchResponsePartHeader]
}
