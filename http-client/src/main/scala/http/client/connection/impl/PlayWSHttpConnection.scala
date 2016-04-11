package http.client.connection.impl

import http.client.connection.HttpConnection
import http.client.request.{ GetRequest, PostRequest }
import http.client.response.HttpResponse
import play.api.http.Writeable
import play.api.libs.ws.WSResponse
import play.api.libs.ws.ning.{ NingWSClient, NingWSRequest }

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

  override def get(getRequest: GetRequest)(implicit ec: ExecutionContext): Future[HttpResponse] = {

    val r = client
      .url(getRequest.relativeUrl)
      .withHeaders(getRequest.headers: _*)
      .withQueryString(queryStringToSeq(getRequest.queryString): _*)
      .withMethod(GET)

    val n = r.asInstanceOf[NingWSRequest]
    println("request headers = " + n.headers)
    println("request body = " + n.getBody)

    r
      .execute()
      .map(PlayWsHttpResponse.apply)
  }

  override def post[T](postRequest: PostRequest[T])(implicit ec: ExecutionContext, bodyWriteable: Writeable[T]): Future[HttpResponse] = {
    val r = client
      .url(postRequest.relativeUrl)
      .withHeaders(postRequest.headers: _*)
      .withQueryString(queryStringToSeq(postRequest.queryString): _*)
      .withMethod(POST)
      .withBody[T](postRequest.body.getOrElse(null.asInstanceOf[T]))(bodyWriteable)

    val n = r.asInstanceOf[NingWSRequest]
    println("request headers = " + n.headers)
    println("request body = " + new String(n.getBody.getOrElse(Array.empty)))

    r
      .execute()
      .map(PlayWsHttpResponse.apply)
  }
}

