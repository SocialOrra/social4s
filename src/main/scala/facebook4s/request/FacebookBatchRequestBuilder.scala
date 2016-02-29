package facebook4s.request

import facebook4s.api.AccessToken
import facebook4s.connection.FacebookConnectionInformation
import facebook4s.response.{ FacebookBatchResponsePart, FacebookBatchResponse }
import http.client.connection.HttpConnection
import http.client.request.{ HttpBatchRequestBuilder, Request }
import http.client.response.{ BatchResponse, HttpResponse, BatchResponsePart }

import scala.collection.mutable.ListBuffer

object FacebookBatchRequestBuilder {

  val ACCESS_TOKEN = "access_token"
  val FB_BATCH_PATH = ""
  val BATCH = "batch"

  def http(protocol: String, domain: String, version: String, path: String): String =
    s"$protocol://$domain/$version/$path"

  def accessTokenQS(accessToken: AccessToken): (String, Seq[String]) =
    ACCESS_TOKEN -> Seq(accessToken.token)
}

import FacebookBatchRequestBuilder._

class FacebookBatchRequestBuilder(cfg: FacebookConnectionInformation, connection: HttpConnection, accessToken: Option[AccessToken], requests: ListBuffer[Request] = ListBuffer.empty)
    extends HttpBatchRequestBuilder[FacebookBatchResponse, FacebookBatchRequestBuilder](requests, connection, http(cfg.protocol, cfg.graphApiHost, cfg.version, FB_BATCH_PATH)) {

  override protected def maybeRanged(since: Option[Long], until: Option[Long], request: Request): Request =
    if (since.isDefined && until.isDefined) FacebookTimeRangedRequest(since.get, until.get, request)
    else request

  override protected def maybePaginated(paginated: Boolean, request: Request): Request =
    if (paginated) FacebookCursorPaginatedRequest(request)
    else request

  override protected def accumulateCompleteRequest(reqRes: (Request, BatchResponsePart)): (Request, BatchResponsePart) = reqRes match {
    case (req: FacebookPaginatedRequest, res) ⇒ (req.originalRequest, res) // original request so we can group all parts on it later
    case rr                                   ⇒ rr
  }

  override protected def newRequestFromIncompleteRequest(reqRes: (Request, BatchResponsePart)): Request = {
    reqRes._1.asInstanceOf[FacebookPaginatedRequest].nextRequest(reqRes._2)
  }

  override protected def makeParts(requests: Seq[Request]): Seq[(String, Array[Byte])] = {
    accessToken.map { a ⇒ Seq(ACCESS_TOKEN -> a.token.getBytes("utf-8")) }
      .getOrElse(Seq.empty[(String, Array[Byte])]) ++
      Seq(BATCH -> ("[" + requests.map(_.toJson()).mkString(",") + "]").getBytes("utf-8"))
  }

  override protected def fromHttpResponse(wsResponse: HttpResponse): FacebookBatchResponse = {
    FacebookBatchResponse(wsResponse.status, wsResponse.headers, wsResponse.json.validate[Seq[FacebookBatchResponsePart]].getOrElse(Seq.empty))
  }
}
