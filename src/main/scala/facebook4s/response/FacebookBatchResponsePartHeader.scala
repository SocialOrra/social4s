package facebook4s.response

import http.client.response.BatchResponsePartHeader
import play.api.libs.json.Json

/** Represents a batch response header with a name and value.
 */
case class FacebookBatchResponsePartHeader(override val name: String, override val value: String) extends BatchResponsePartHeader

object FacebookBatchResponsePartHeader {
  implicit val batchResponsePartHeaderFmt = Json.format[FacebookBatchResponsePartHeader]
}
