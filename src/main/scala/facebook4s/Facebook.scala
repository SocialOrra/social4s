package facebook4s

import java.nio.ByteBuffer

import com.ning.http.client.FluentCaseInsensitiveStringsMap
import com.ning.http.client.multipart.{ ByteArrayPart, MultipartUtils }
import play.api.libs.ws.{ WSRequest, WSResponse }
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

  private[facebook4s] def queryStringToSeq(fields: Map[String, Seq[String]]): Seq[(String, String)] =
    fields.flatMap(keyAndValues ⇒ {
      val key = keyAndValues._1
      keyAndValues._2.map(value ⇒ (key, value)).toList
    }).toSeq

  private[facebook4s] def buildGet(url: String, fields: Map[String, Seq[String]])(implicit accessToken: AccessToken): WSRequest = {

    WSClient.client
      .url(http(FB_GRAPH_DOMAIN, FB_API_VERSION, url))
      .withQueryString(queryStringToSeq(fields) :+ accessTokenQS(accessToken): _*)
      .withMethod(GET)
  }

  private[facebook4s] def buildPost[T](url: String, body: T, fields: Map[String, Seq[String]])(
    implicit accessToken: AccessToken, bodyWriteable: Writeable[T]): WSRequest = {
    WSClient.client
      .url(http(FB_GRAPH_DOMAIN, FB_API_VERSION, url))
      .withQueryString(queryStringToSeq(fields) :+ accessTokenQS(accessToken): _*)
      .withMethod(POST)
      .withBody[T](body)(bodyWriteable)
  }

  private[facebook4s] def http(domain: String, version: String, path: String): String =
    s"$PROTOCOL://$domain/v$version/$path"

  private[facebook4s] def accessTokenQS(accessToken: AccessToken): (String, String) =
    ACCESS_TOKEN -> accessToken.token

  private[facebook4s] def boundary: String = {
    // TODO: generate this
    "------------------------022a3c34ef7f73dd"
  }
}

class FacebookConnection {

  import FacebookConnection._

  def get(url: String, fields: Map[String, Seq[String]])(implicit accessToken: AccessToken): Future[WSResponse] =
    buildGet(url, fields).execute(GET)

  def post[T](url: String, body: T, fields: Map[String, Seq[String]])(implicit accessToken: AccessToken, writeable: Writeable[T]): Future[WSResponse] =
    buildPost(url, body, fields).execute(POST)

  def batch(parts: Seq[(String, Array[Byte])]): Future[WSResponse] =
    buildBatch(parts).execute(POST)

  def shutdown(): Unit =
    WSClient.shutdown()

  def buildBatch(parts: Seq[(String, Array[Byte])]): WSRequest = {

    val byteArrayParts = parts.map(p ⇒ new ByteArrayPart(p._1, p._2))
    val headers = new FluentCaseInsensitiveStringsMap().add("Content-Type", s"multipart/form-data; boundary=$boundary")

    val request = MultipartUtils.newMultipartBody(java.util.Arrays.asList(byteArrayParts: _*), headers)
    val buf = ByteBuffer.allocate(request.getContentLength.toInt)
    request.read(buf)

    WSClient.client
      .url(http(FB_GRAPH_DOMAIN, FB_API_VERSION, FB_BATCH_PATH))
      .withHeaders(("Content-Type", s"multipart/form-data; boundary=$boundary"))
      .withMethod(POST)
      .withBody(buf.array())
  }
}

