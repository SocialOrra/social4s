package http.client.connection

import http.client.request.HttpRequest
import http.client.response.HttpResponse
import play.api.http.Writeable

import scala.concurrent.{ExecutionContext, Future}

trait HttpConnection {
  def makeRequest(request: HttpRequest)(implicit ec: ExecutionContext): Future[HttpResponse]
  def shutdown(): Unit
}
