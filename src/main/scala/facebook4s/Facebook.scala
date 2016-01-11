package facebook4s

import java.nio.ByteBuffer

import com.ning.http.client.FluentCaseInsensitiveStringsMap
import com.ning.http.client.multipart.{ ByteArrayPart, MultipartUtils }
import play.api.libs.ws.WSResponse
import play.api.libs.ws.ning._
import play.api.http.Writeable

import scala.concurrent.Future

private[facebook4s] object WSClient {
  implicit val client = NingWSClient()
  def shutdown() = client.close()
}

object FacebookConnection {
  val GET = "GET"
  val POST = "POST"

  // prod
  val FB_GRAPH_DOMAIN = "graph.facebook.com"
  val PROTOCOL = "https"

  // testing
  //val FB_GRAPH_DOMAIN = "localhost:8080"
  //val PROTOCOL = "http"

  val FB_API_VERSION = "2.5"
  val FB_BATCH_PATH = ""
  val ACCESS_TOKEN = "access_token"
  val BATCH = "batch"
}

class FacebookConnection {

  import FacebookConnection._

  def get(url: String, fields: Map[String, Seq[String]])(implicit accessToken: AccessToken): Future[WSResponse] = {
    val qs = fields.flatMap(keyAndValues ⇒ {
      val key = keyAndValues._1
      keyAndValues._2.map(value ⇒ (key, value)).toList
    }).toSeq

    WSClient.client
      .url(http(FB_GRAPH_DOMAIN, FB_API_VERSION, url))
      .withQueryString(qs :+ accessTokenQS(accessToken): _*)
      .execute(GET)
  }

  def post[T](url: String, body: T, fields: Map[String, Seq[String]])(
    implicit accessToken: AccessToken, bodyWriteable: Writeable[T]): Future[WSResponse] = {
    val qs = fields.flatMap(keyAndValues ⇒ {
      val key = keyAndValues._1
      keyAndValues._2.map(value ⇒ (key, value)).toList
    }).toSeq

    WSClient.client
      .url(http(FB_GRAPH_DOMAIN, FB_API_VERSION, url))
      .withQueryString(qs :+ accessTokenQS(accessToken): _*)
      .withBody[T](body)(bodyWriteable)
      .execute(POST)
  }

  def batch(parts: Seq[(String, Array[Byte])]): Future[WSResponse] = {

    val byteArrayParts = parts.map(p ⇒ new ByteArrayPart(p._1, p._2))
    val headers = new FluentCaseInsensitiveStringsMap().add("Content-Type", s"multipart/form-data; boundary=${boundary}")

    val request = MultipartUtils.newMultipartBody(java.util.Arrays.asList(byteArrayParts: _*), headers)
    val buf = ByteBuffer.allocate(request.getContentLength.toInt)
    request.read(buf)

    WSClient.client
      .url(http(FB_GRAPH_DOMAIN, FB_API_VERSION, FB_BATCH_PATH))
      .withHeaders(("Content-Type", s"multipart/form-data; boundary=${boundary}"))
      .post(buf.array())
  }

  def shutdown() = WSClient.shutdown()

  private def http(domain: String, version: String, path: String): String =
    s"$PROTOCOL://$domain/v$version/$path"

  private def accessTokenQS(accessToken: AccessToken): (String, String) =
    ACCESS_TOKEN -> accessToken.token

  private def boundary: String = {
    // TODO: generate this
    "------------------------022a3c34ef7f73dd"
  }
}

