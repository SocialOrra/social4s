package facebook4s.connection

import play.api.libs.ws.ning._

private[facebook4s] class WSClient {
  implicit val get = NingWSClient()
  def shutdown() = get.close()
}

