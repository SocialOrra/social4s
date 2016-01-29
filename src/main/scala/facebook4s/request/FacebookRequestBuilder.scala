package facebook4s.request

import facebook4s.api.AccessToken
import facebook4s.connection.FacebookConnection
import facebook4s.response.{ FacebookBatchResponse, FacebookBatchResponsePart }

import scala.collection.mutable.ListBuffer
import scala.concurrent.{ ExecutionContext, Future }

case class FacebookRequestBuilder(requests: ListBuffer[Request] = ListBuffer.empty) {

  import FacebookConnection._

  def get(relativeUrl: String, queryString: Map[String, Seq[String]], since: Option[Long], until: Option[Long], accessToken: Option[AccessToken]): FacebookRequestBuilder =
    batch(maybeRanged(since, until, GetRequest(relativeUrl, queryString, accessToken)))

  def get(relativeUrl: String, queryString: Map[String, Seq[String]], paginate: Boolean, accessToken: Option[AccessToken]): FacebookRequestBuilder =
    batch(maybePaginated(paginate, GetRequest(relativeUrl, queryString, accessToken)))

  def post(relativeUrl: String, body: Option[String], queryString: Map[String, Seq[String]], since: Option[Long], until: Option[Long], accessToken: Option[AccessToken]): FacebookRequestBuilder =
    batch(maybeRanged(since, until, PostRequest(relativeUrl, queryString, accessToken, body)))

  def post(relativeUrl: String, body: Option[String], queryString: Map[String, Seq[String]], paginated: Boolean, accessToken: Option[AccessToken]): FacebookRequestBuilder =
    batch(maybePaginated(paginated, PostRequest(relativeUrl, queryString, accessToken, body)))

  def execute(implicit facebookConnection: FacebookConnection, accessToken: Option[AccessToken] = None, ec: ExecutionContext): Future[FacebookBatchResponse] =
    facebookConnection
      // assemble request parts and send it off
      .batch(makeParts(accessToken, requests))
      // map the response to our internal type
      .map(FacebookBatchResponse.fromWSResponse)

  def executeWithPaginationWithoutMerging(implicit facebookConnection: FacebookConnection, accessToken: Option[AccessToken] = None, ec: ExecutionContext): Future[Seq[(Request, FacebookBatchResponsePart)]] =
    _executeWithPagination(requests)

  def executeWithPagination(implicit facebookConnection: FacebookConnection, accessToken: Option[AccessToken] = None, ec: ExecutionContext): Future[Map[Request, Seq[FacebookBatchResponsePart]]] = {
    _executeWithPagination(requests) map { requestsAndResponses ⇒
      requestsAndResponses
        .groupBy(_._1)
        .mapValues(_.map(_._2))
        .map { requestAndResponseParts ⇒
          // TODO: could there be no parts?
          val request = requestAndResponseParts._1
          val parts = requestAndResponseParts._2
          //val combinedBody: String = parts.map(p ⇒ p.bodyJson.validate[JsObject].get).foldLeft(JsObject(Seq.empty))(_ deepMerge _).toString()
          //val combinedPart = FacebookBatchResponsePart(code = parts.head.code, headers = parts.head.headers, body = combinedBody)
          (request, parts)
        }
    }
  }

  private def _executeWithPagination(requests: Seq[Request], completedRequests: Seq[(Request, FacebookBatchResponsePart)] = Seq.empty)(implicit facebookConnection: FacebookConnection, accessToken: Option[AccessToken] = None, ec: ExecutionContext): Future[Seq[(Request, FacebookBatchResponsePart)]] = {

    val parts = makeParts(accessToken, requests)

    facebookConnection.batch(parts).map { rawResponse ⇒

      val response = FacebookBatchResponse.fromWSResponse(rawResponse)

      val responses = requests
        // group up each request with it's corresponding response
        .zip(response.parts)
        // create two groups: complete, incomplete
        // note that  non-200 responses are considered complete
        .groupBy(isRequestComplete)

      val complete = responses.getOrElse(true, Seq.empty).map(accumulateCompleteRequest)
      val incompleteResponses = responses.getOrElse(false, Seq.empty).map(accumulateCompleteRequest)
      val newRequests = responses.getOrElse(false, Seq.empty).map(newRequestFromIncompleteRequest)

      (complete ++ incompleteResponses, newRequests)

    } flatMap {
      case (complete, incomplete) if incomplete.nonEmpty ⇒
        _executeWithPagination(incomplete, completedRequests ++ complete)
      case (complete, _) ⇒
        Future.successful { completedRequests ++ complete }
    }
  }

  private def accumulateCompleteRequest(reqRes: (Request, FacebookBatchResponsePart)): (Request, FacebookBatchResponsePart) = reqRes match {
    case (req: PaginatedRequest[_, _], res) ⇒ (req.originalRequest, res) // original request so we can group all parts on it later
    case rr                                 ⇒ rr
  }

  private def newRequestFromIncompleteRequest(reqRes: (Request, FacebookBatchResponsePart)): Request = {
    reqRes._1.asInstanceOf[PaginatedRequest[_, _]].nextRequest(reqRes._2)
  }

  private def isRequestComplete(reqRes: (Request, FacebookBatchResponsePart)): Boolean = {
    val request = reqRes._1
    val response = reqRes._2

    if (response.code == 200) {
      request.isComplete(reqRes._2)
    } else {
      // error
      true
    }
  }

  private def makeParts(accessToken: Option[AccessToken], requests: Seq[Request]): Seq[(String, Array[Byte])] = {
    accessToken.map { a ⇒ Seq(ACCESS_TOKEN -> a.token.getBytes("utf-8")) }
      .getOrElse(Seq.empty[(String, Array[Byte])]) ++
      Seq(BATCH -> ("[" + requests.map(_.toJson()).mkString(",") + "]").getBytes("utf-8"))
  }

  private def maybeRanged(since: Option[Long], until: Option[Long], request: Request): Request =
    if (since.isDefined && until.isDefined) TimeRangedRequest(since.get, until.get, request)
    else request

  private def maybePaginated(paginated: Boolean, request: Request): Request =
    if (paginated) CursorPaginatedRequest(request)
    else request

  private def batch(request: Request): FacebookRequestBuilder = {
    requests += request
    this
  }
}

