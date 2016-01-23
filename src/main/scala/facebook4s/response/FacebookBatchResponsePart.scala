package facebook4s.response

import play.api.libs.json.Json

/** Represents part of a Facebook batch response, where the body is a JSON object.
 */
case class FacebookBatchResponsePart(code: Int, headers: Seq[FacebookBatchResponsePartHeader], body: String) {
  lazy val bodyJson = Json.parse(body)
}

object FacebookBatchResponsePart {
  implicit val batchResponsePartFmt = Json.format[FacebookBatchResponsePart]
}
