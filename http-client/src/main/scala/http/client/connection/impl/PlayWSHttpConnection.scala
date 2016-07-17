package http.client.connection.impl

import http.client.connection.HttpConnection
import http.client.request.HttpRequest
import http.client.response.{HttpHeader, HttpResponse}
import play.api.libs.ws.WSResponse
import play.api.libs.ws.ning.NingWSClient

import scala.concurrent.{ExecutionContext, Future}

private[impl] case class PlayWsHttpResponse(status: Int, headers: Seq[HttpHeader], response: WSResponse) extends HttpResponse {
  override val statusText = response.statusText
  override val body = response.body
  override val bodyAsBytes = response.bodyAsBytes
  override def json = response.json
}

private[impl] object PlayWsHttpResponse {
  def apply(wsReponse: WSResponse): PlayWsHttpResponse =
    PlayWsHttpResponse(
      wsReponse.status,
      // turn Map[String, Seq[String]] into Seq[HttpHeader]
      wsReponse.allHeaders.flatMap { kv ⇒ kv._2.map { v ⇒ HttpHeader(kv._1, v) } }.toSeq,
      wsReponse)
}

class PlayWSHttpConnection extends HttpConnection {

  implicit val client = NingWSClient()

  private def queryStringToSeq(fields: Map[String, Seq[String]]): Seq[(String, String)] =
    fields.flatMap(keyAndValues ⇒ {
      val key = keyAndValues._1
      keyAndValues._2.map(value ⇒ (key, value)).toList
    }).toSeq

  override def shutdown() = client.close()

  override def makeRequest(request: HttpRequest)(implicit ec: ExecutionContext): Future[HttpResponse] = {

    val r = client
      .url(request.baseUrl + request.relativeUrl)
      .withHeaders(request.headers.map({ h ⇒ h.name → h.value }): _*)
      .withQueryString(queryStringToSeq(request.queryString): _*)
      .withMethod(request.method.name)

    val req = if (request.body.isDefined) {
      val r2 = r.withBody(request.body.get)
      r2.execute()
    } else {
      r.execute()
    }

    req.map(PlayWsHttpResponse.apply)
  }
}

