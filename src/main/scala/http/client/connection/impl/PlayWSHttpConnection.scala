package http.client.connection.impl

import java.nio.ByteBuffer

import com.ning.http.client.FluentCaseInsensitiveStringsMap
import com.ning.http.client.multipart.{ ByteArrayPart, MultipartUtils }
import http.client.connection.HttpConnection
import http.client.request.{ BatchRequest, PostRequest, GetRequest }
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

trait RequestsPerSecond extends HttpConnection {

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

  private val boundary: String =
    "------------------------" + scala.util.Random.alphanumeric.take(16).mkString

  override def shutdown() = client.close()

  override def get(getRequest: GetRequest)(implicit ec: ExecutionContext): Future[HttpResponse] = {

    client
      .url(getRequest.relativeUrl)
      .withQueryString(queryStringToSeq(getRequest.queryString): _*)
      .withMethod(GET)
      .execute()
      .map(PlayWsHttpResponse.apply)
  }

  override def post[T](postRequest: PostRequest[T])(implicit ec: ExecutionContext, bodyWriteable: Writeable[T]): Future[HttpResponse] = {
    client
      .url(postRequest.relativeUrl)
      .withQueryString(queryStringToSeq(postRequest.queryString): _*)
      .withMethod(POST)
      .withBody[T](postRequest.body.getOrElse(null.asInstanceOf[T]))(bodyWriteable)
      .execute()
      .map(PlayWsHttpResponse.apply)
  }

  override def batch(batchRequest: BatchRequest)(implicit ec: ExecutionContext): Future[HttpResponse] = {

    val byteArrayParts = batchRequest.parts.map(p ⇒ new ByteArrayPart(p._1, p._2))
    val headers = new FluentCaseInsensitiveStringsMap().add("Content-Type", s"multipart/form-data; boundary=$boundary")

    val request = MultipartUtils.newMultipartBody(java.util.Arrays.asList(byteArrayParts: _*), headers)
    val buf = ByteBuffer.allocate(request.getContentLength.toInt)
    request.read(buf)

    client
      .url(batchRequest.url)
      .withHeaders(("Content-Type", s"multipart/form-data; boundary=$boundary"))
      .withMethod(POST)
      .withBody(buf.array())
      .execute()
      .map(PlayWsHttpResponse.apply)
  }
}
