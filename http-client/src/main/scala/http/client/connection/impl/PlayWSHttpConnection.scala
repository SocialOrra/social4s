package http.client.connection.impl

import http.client.connection.HttpConnection
import http.client.request.{ GetRequest, PostRequest }
import http.client.response.HttpResponse
import play.api.http.Writeable
import play.api.libs.ws.WSResponse
import play.api.libs.ws.ning.NingWSClient

import scala.concurrent.{ ExecutionContext, Future }

private[impl] case class PlayWsHttpResponse(status: Int, headers: Map[String, Seq[String]], response: WSResponse) extends HttpResponse {
  override def json = response.json
}

private[impl] object PlayWsHttpResponse {
  def apply(wsReponse: WSResponse): PlayWsHttpResponse = PlayWsHttpResponse(wsReponse.status, wsReponse.allHeaders, wsReponse)
}

class PlayWSHttpConnection extends HttpConnection {

  val GET = "GET"
  val POST = "POST"

  implicit val client = NingWSClient()

  private def queryStringToSeq(fields: Map[String, Seq[String]]): Seq[(String, String)] =
    fields.flatMap(keyAndValues ⇒ {
      val key = keyAndValues._1
      keyAndValues._2.map(value ⇒ (key, value)).toList
    }).toSeq

  override def shutdown() = client.close()

  override def get(getRequest: GetRequest)(implicit ec: ExecutionContext): Future[HttpResponse] = {

    client
      .url(getRequest.relativeUrl)
      .withHeaders(getRequest.headers: _*)
      .withQueryString(queryStringToSeq(getRequest.queryString): _*)
      .withMethod(GET)
      .execute()
      .map(PlayWsHttpResponse.apply)
  }

  override def post[T](postRequest: PostRequest[T])(implicit ec: ExecutionContext, bodyWriteable: Writeable[T]): Future[HttpResponse] = {
    client
      .url(postRequest.relativeUrl)
      .withHeaders(postRequest.headers: _*)
      .withQueryString(queryStringToSeq(postRequest.queryString): _*)
      .withMethod(POST)
      .withBody[T](postRequest.body.getOrElse(null.asInstanceOf[T]))(bodyWriteable)
      .execute()
      .map(PlayWsHttpResponse.apply)
  }
}

