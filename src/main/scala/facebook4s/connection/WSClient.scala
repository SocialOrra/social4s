package facebook4s.connection

import play.api.libs.ws.ning._

private[facebook4s] object WSClient {
  implicit val client = NingWSClient()
  def shutdown() = client.close()
}

