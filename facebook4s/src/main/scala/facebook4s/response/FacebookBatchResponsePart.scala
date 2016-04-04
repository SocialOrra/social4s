package facebook4s.response

import http.client.response.BatchResponsePart
import play.api.libs.json.Json

/** Represents part of a Facebook batch response, where the body is a JSON object.
 */
case class FacebookBatchResponsePart(override val code: Int, override val headers: Seq[FacebookBatchResponsePartHeader], override val body: String)
    extends BatchResponsePart {
  lazy val bodyJson = Json.parse(body)
}

object FacebookBatchResponsePart {
  implicit val batchResponsePartFmt = Json.format[FacebookBatchResponsePart]
}
