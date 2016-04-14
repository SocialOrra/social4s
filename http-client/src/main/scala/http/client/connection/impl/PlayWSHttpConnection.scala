package http.client.connection.impl

import http.client.connection.HttpConnection
import http.client.request.Request
import http.client.response.HttpResponse
import play.api.libs.ws.WSResponse
import play.api.libs.ws.ning.NingWSClient

import scala.concurrent.{ ExecutionContext, Future }

private[impl] case class PlayWsHttpResponse(status: Int, headers: Map[String, Seq[String]], response: WSResponse) extends HttpResponse {
  override def statusText = response.statusText
  override def body = response.body
  override def bodyAsBytes = response.bodyAsBytes
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

  override def makeRequest(request: Request)(implicit ec: ExecutionContext): Future[HttpResponse] = {
    val r = client
      .url(request.relativeUrl)
      .withHeaders(request.headers: _*)
      .withQueryString(queryStringToSeq(request.queryString): _*)
      .withMethod(request.method.name)

    request.body.map(b ⇒ r.withBody(b))

    r.execute()
      .map(PlayWsHttpResponse.apply)
  }
}

