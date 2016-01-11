package facebook4s

import java.nio.ByteBuffer

import com.ning.http.client.FluentCaseInsensitiveStringsMap
import com.ning.http.client.multipart.{ ByteArrayPart, MultipartUtils }
import play.api.libs.json.{ JsString, JsObject }
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

trait HttpMethod { val name: String }
case object GetMethod extends HttpMethod { override val name = "GET" }
case object PostMethod extends HttpMethod { override val name = "POST" }

trait Request {
  val method: HttpMethod
  val relativeUrl: String
  val queryString: Map[String, Seq[String]]
  val accessToken: Option[AccessToken]

  def toJson: String

  protected def queryStringAsStringWithToken = (queryString ++ accessToken.map(accessTokenQS))
    .flatMap { keyAndValues ⇒
      val key = keyAndValues._1
      keyAndValues._2.map(value ⇒ s"$key=$value").toList
    }
    .mkString("&")

  protected def accessTokenQS(accessToken: AccessToken): (String, Seq[String]) =
    FacebookConnection.ACCESS_TOKEN -> Seq(accessToken.token)

  protected def maybeQueryString: String = {
    if (queryString.nonEmpty) "?" + queryStringAsStringWithToken
    else ""
  }
}

case class GetRequest(relativeUrl: String, queryString: Map[String, Seq[String]], accessToken: Option[AccessToken], method: HttpMethod = GetMethod) extends Request {
  override def toJson: String = {
    JsObject(Seq(
      "method" -> JsString(method.name),
      "relative_url" -> JsString(relativeUrl + maybeQueryString))).toString()
  }
}

case class PostRequest(relativeUrl: String, queryString: Map[String, Seq[String]], accessToken: Option[AccessToken], body: Option[String], method: HttpMethod = PostMethod) extends Request {

  override def toJson: String = {
    JsObject(Seq(
      "method" -> JsString(method.name),
      "relative_url" -> JsString(relativeUrl + maybeQueryString)) ++
      body.map(b ⇒ Seq("body" -> JsString(b))).getOrElse(Seq.empty) // TODO: URLEncode() the body string, needed?
      ).toString()
  }
}

object FacebookRequestBuilder {
  implicit def writeableToSomeWriteable[T](writeable: Writeable[T]): Option[Writeable[T]] = Some(writeable)
}

case class FacebookRequestBuilder(requests: Seq[Request] = Seq.empty) {

  import FacebookConnection._

  def get(relativeUrl: String, queryString: Map[String, Seq[String]], accessToken: Option[AccessToken]) =
    batch(GetRequest(relativeUrl, queryString, accessToken))

  def post(relativeUrl: String, body: Option[String], queryString: Map[String, Seq[String]], accessToken: Option[AccessToken]) =
    batch(PostRequest(relativeUrl, queryString, accessToken, body))

  def execute(implicit facebookConnection: FacebookConnection, accessToken: Option[AccessToken] = None): Future[WSResponse] = {

    val accessTokenPart = accessToken.map { a ⇒
      Seq(ACCESS_TOKEN -> a.token.getBytes("utf-8"))
    }.getOrElse(Seq.empty[(String, Array[Byte])])

    val batchPart = BATCH -> ("[" + requests.map(_.toJson).mkString(",") + "]").getBytes("utf-8")

    facebookConnection.batch(accessTokenPart :+ batchPart)
  }

  private def batch(request: Request) = copy(requests :+ request)
}

object FacebookMarketingApi {

  implicit class FacebookAdsInsightsApi(requestBuilder: FacebookRequestBuilder) {
    def adInsights(adId: String, accessToken: Option[AccessToken] = None) = requestBuilder.get(s"$adId/insights", Map.empty, accessToken)
    def campaignInsights(campaignId: String, accessToken: Option[AccessToken] = None) = requestBuilder.get(s"$campaignId/insights", Map.empty, accessToken)
    def adSetInsights(adSetId: String, accessToken: Option[AccessToken] = None) = requestBuilder.get(s"$adSetId/insights", Map.empty, accessToken)
  }
}

object FacebookGraphApi {

  implicit class FacebookGraphApi(requestBuilder: FacebookRequestBuilder) {
    def me(fields: Map[String, Seq[String]] = Map.empty, accessToken: Option[AccessToken] = None) = requestBuilder.get("me", Map.empty, accessToken)
    def friends(fields: Map[String, Seq[String]] = Map.empty, accessToken: Option[AccessToken] = None) = requestBuilder.get("me/friends", fields, accessToken)
    def pageInsights(pageId: String, fields: Map[String, Seq[String]] = Map.empty, accessToken: Option[AccessToken] = None) = requestBuilder.get("me/friends", fields, accessToken)
  }
}
