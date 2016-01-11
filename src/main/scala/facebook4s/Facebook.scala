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

