package http.client.connection

import play.api.http.Writeable
import play.api.libs.ws.WSResponse

import scala.concurrent.{ ExecutionContext, Future }

trait HttpConnection[D] {
  def get(url: String, fields: Map[String, Seq[String]])(implicit data: D): Future[WSResponse]
  def post[T](url: String, body: T, fields: Map[String, Seq[String]])(implicit data: D, writeable: Writeable[T]): Future[WSResponse]
  def batch(parts: Seq[(String, Array[Byte])])(implicit ec: ExecutionContext): Future[WSResponse]
  def shutdown(): Unit
}
