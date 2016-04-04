package http.client.connection

import http.client.request.{ BatchRequest, PostRequest, GetRequest }
import http.client.response.HttpResponse
import play.api.http.Writeable

import scala.concurrent.{ ExecutionContext, Future }

trait HttpConnection {
  def get(getRequest: GetRequest)(implicit ec: ExecutionContext): Future[HttpResponse]
  def post[T](postRequest: PostRequest[T])(implicit ec: ExecutionContext, writeable: Writeable[T]): Future[HttpResponse]
  def batch(batchRequest: BatchRequest)(implicit ec: ExecutionContext): Future[HttpResponse]
  def shutdown(): Unit
}
