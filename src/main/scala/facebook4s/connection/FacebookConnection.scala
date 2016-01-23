package facebook4s.connection

import java.nio.ByteBuffer

import com.ning.http.client.FluentCaseInsensitiveStringsMap
import com.ning.http.client.multipart.{ ByteArrayPart, MultipartUtils }
import facebook4s.api.AccessToken
import play.api.http.Writeable
import play.api.libs.ws.{ WSRequest, WSResponse }

import scala.concurrent.{ ExecutionContext, Future }

object FacebookConnection {

  val GET = "GET"
  val POST = "POST"
  val FB_BATCH_PATH = ""
  val ACCESS_TOKEN = "access_token"
  val BATCH = "batch"

  private[facebook4s] def queryStringToSeq(fields: Map[String, Seq[String]]): Seq[(String, String)] =
    fields.flatMap(keyAndValues ⇒ {
      val key = keyAndValues._1
      keyAndValues._2.map(value ⇒ (key, value)).toList
    }).toSeq

  private[facebook4s] def buildGet(url: String, fields: Map[String, Seq[String]])(implicit accessToken: AccessToken, cfg: FacebookConnectionInformation): WSRequest = {

    WSClient.client
      .url(http(cfg.protocol, cfg.graphApiHost, cfg.version, url))
      .withQueryString(queryStringToSeq(fields) :+ accessTokenQS(accessToken): _*)
      .withMethod(GET)
  }

  private[facebook4s] def buildPost[T](url: String, body: T, fields: Map[String, Seq[String]])(
    implicit accessToken: AccessToken, cfg: FacebookConnectionInformation, bodyWriteable: Writeable[T]): WSRequest = {
    WSClient.client
      .url(http(cfg.protocol, cfg.graphApiHost, cfg.version, url))
      .withQueryString(queryStringToSeq(fields) :+ accessTokenQS(accessToken): _*)
      .withMethod(POST)
      .withBody[T](body)(bodyWriteable)
  }

  private[facebook4s] def buildBatch(parts: Seq[(String, Array[Byte])])(implicit cfg: FacebookConnectionInformation): WSRequest = {

    val byteArrayParts = parts.map(p ⇒ new ByteArrayPart(p._1, p._2))
    val headers = new FluentCaseInsensitiveStringsMap().add("Content-Type", s"multipart/form-data; boundary=$boundary")

    val request = MultipartUtils.newMultipartBody(java.util.Arrays.asList(byteArrayParts: _*), headers)
    val buf = ByteBuffer.allocate(request.getContentLength.toInt)
    request.read(buf)

    WSClient.client
      .url(http(cfg.protocol, cfg.graphApiHost, cfg.version, FB_BATCH_PATH))
      .withHeaders(("Content-Type", s"multipart/form-data; boundary=$boundary"))
      .withMethod(POST)
      .withBody(buf.array())
  }

  private[facebook4s] def http(protocol: String, domain: String, version: String, path: String): String =
    s"$protocol://$domain/$version/$path"

  private[facebook4s] def accessTokenQS(accessToken: AccessToken): (String, String) =
    ACCESS_TOKEN -> accessToken.token

  private[facebook4s] def boundary: String = {
    // TODO: generate this
    "------------------------022a3c34ef7f73dd"
  }
}

class FacebookConnection(implicit cfg: FacebookConnectionInformation) {

  import FacebookConnection._

  def get(url: String, fields: Map[String, Seq[String]])(implicit accessToken: AccessToken): Future[WSResponse] =
    buildGet(url, fields).execute(GET)

  def post[T](url: String, body: T, fields: Map[String, Seq[String]])(implicit accessToken: AccessToken, writeable: Writeable[T]): Future[WSResponse] =
    buildPost(url, body, fields).execute(POST)

  def batch(parts: Seq[(String, Array[Byte])])(implicit ec: ExecutionContext): Future[WSResponse] =
    buildBatch(parts).execute(POST)

  def shutdown(): Unit =
    WSClient.shutdown()
}

