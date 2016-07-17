package facebook4s.request

import java.nio.ByteBuffer

import com.ning.http.client.FluentCaseInsensitiveStringsMap
import com.ning.http.client.multipart.{ByteArrayPart, MultipartUtils}
import facebook4s.api.AccessToken
import facebook4s.connection.FacebookConnectionInformation
import facebook4s.response.{FacebookBatchResponse, FacebookBatchResponsePart}
import http.client.connection.HttpConnection
import http.client.method.PostMethod
import http.client.request.{HttpBatchRequestBuilder, HttpRequest, TrueCompletionEvaluation}
import http.client.response.{HttpHeader, HttpResponse}

import scala.collection.mutable.ListBuffer

object FacebookBatchRequestBuilder {

  val ACCESS_TOKEN = "access_token"
  val FB_BATCH_PATH = ""
  val BATCH = "batch"

  def http(protocol: String, domain: String, version: String, path: String): String =
    s"$protocol://$domain/$version/$path"

  def accessTokenQS(accessToken: AccessToken): (String, Seq[String]) =
    ACCESS_TOKEN → Seq(accessToken.token)
}

import FacebookBatchRequestBuilder._

class FacebookBatchRequestBuilder(cfg: FacebookConnectionInformation, connection: HttpConnection, accessToken: Option[AccessToken], requests: ListBuffer[HttpRequest] = ListBuffer.empty)
    extends HttpBatchRequestBuilder[FacebookBatchResponse, FacebookBatchResponsePart, FacebookBatchRequestBuilder](requests, connection, http(cfg.protocol, cfg.graphApiHost, cfg.version, FB_BATCH_PATH)) {

  private val boundary: String =
    "------------------------" + scala.util.Random.alphanumeric.take(16).mkString

  override protected def maybeRanged(since: Option[Long], until: Option[Long], request: HttpRequest): HttpRequest =
    if (since.isDefined && until.isDefined) FacebookTimeRangedRequest(since.get, until.get, request)
    else request

  override protected def maybePaginated(paginated: Boolean, request: HttpRequest): HttpRequest =
    if (paginated) FacebookCursorPaginatedRequest(request)
    else request

  override protected def accumulateCompleteRequest(reqRes: (HttpRequest, FacebookBatchResponsePart)): (HttpRequest, FacebookBatchResponsePart) = reqRes match {
    case (req: FacebookPaginatedRequest, res) ⇒ (req.originalRequest, res) // original request so we can group all parts on it later
    case rr                                   ⇒ rr
  }

  override protected def newRequestFromIncompleteRequest(reqRes: (HttpRequest, FacebookBatchResponsePart)): HttpRequest = {
    reqRes._1.asInstanceOf[FacebookPaginatedRequest].nextRequest(reqRes._2)
  }

  override protected def makeBatchRequestBody(requests: Seq[HttpRequest]): Array[Byte] = {
    val parts = accessToken.map { a ⇒ Seq(ACCESS_TOKEN → a.token.getBytes("utf-8")) }
      .getOrElse(Seq.empty[(String, Array[Byte])]) ++
      Seq(BATCH → ("[" + requests.map(_.toJson()).mkString(",") + "]").getBytes("utf-8"))

    val s = accessToken.map { a ⇒ Seq(ACCESS_TOKEN → a.token) }
      .getOrElse(Seq.empty[(String, String)]) ++
      Seq(BATCH → ("[" + requests.map(_.toJson()).mkString(",") + "]"))

    val byteArrayParts = parts.map(p ⇒ new ByteArrayPart(p._1, p._2))
    val headers = new FluentCaseInsensitiveStringsMap().add("Content-Type", s"multipart/form-data; boundary=$boundary")

    val request = MultipartUtils.newMultipartBody(java.util.Arrays.asList(byteArrayParts: _*), headers)
    val buf = ByteBuffer.allocate(request.getContentLength.toInt)
    request.read(buf)

    buf.array()
  }

  override protected def makeBatchRequest(batchUrl: String, _body: Array[Byte]): HttpRequest = {
    val _headers = Seq(HttpHeader("Content-Type", s"multipart/form-data; boundary=$boundary"))
    new HttpRequest {
      val completionEvaluator = TrueCompletionEvaluation
      val method = PostMethod
      val queryString = Map.empty[String, Seq[String]]
      val body = Some(_body)
      val headers = _headers
      val baseUrl = batchUrl
      val relativeUrl = "" // No need to use one since these are batch requests
      def toJson(extraQueryStringParams: Map[String, Seq[String]]): String = ""
    }
  }

  override protected def fromHttpResponse(wsResponse: HttpResponse): FacebookBatchResponse = {
    FacebookBatchResponse(
      wsResponse.status,
      wsResponse.statusText,
      wsResponse.headers,
      wsResponse.bodyAsBytes,
      wsResponse.json,
      wsResponse.json.validate[Seq[FacebookBatchResponsePart]].getOrElse(Seq.empty))
  }
}
