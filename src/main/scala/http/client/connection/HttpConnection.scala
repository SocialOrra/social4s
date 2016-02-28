package http.client.connection

import http.client.request.{ BatchRequest, PostRequest, GetRequest }
import play.api.http.Writeable
import play.api.libs.ws.WSResponse

import scala.concurrent.{ ExecutionContext, Future }

trait HttpConnection {
  def get(getRequest: GetRequest)(implicit ec: ExecutionContext): Future[WSResponse]
  def post[T](postRequest: PostRequest[T])(implicit ec: ExecutionContext, writeable: Writeable[T]): Future[WSResponse]
  def batch(batchRequest: BatchRequest)(implicit ec: ExecutionContext): Future[WSResponse]
  def shutdown(): Unit
}
