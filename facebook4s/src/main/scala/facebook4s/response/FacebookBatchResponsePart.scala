package facebook4s.response

import http.client.response.{HttpHeader, HttpResponse}
import play.api.libs.json.Json

/** Represents part of a Facebook batch response, where the body is a JSON object.
 */
case class FacebookBatchResponsePart(code: Int, override val headers: Seq[HttpHeader], override val body: String)
    extends HttpResponse {
  override val status = code
  lazy val json = Json.parse(body)
  override val statusText: String = status.toString
  override val bodyAsBytes: Array[Byte] = body.getBytes("utf-8")
}

object FacebookBatchResponsePart {
  implicit val batchResponsePartFmt = Json.format[FacebookBatchResponsePart]
}
