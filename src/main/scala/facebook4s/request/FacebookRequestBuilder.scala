package facebook4s.request

import facebook4s.api.AccessToken
import facebook4s.connection.FacebookConnection
import facebook4s.response.{ FacebookBatchResponse, FacebookBatchResponsePart, FacebookPagingInfo }
import play.api.http.Writeable

import scala.collection.mutable.ListBuffer
import scala.concurrent.{ ExecutionContext, Future }

object FacebookRequestBuilder {

  trait Implicits {
    implicit def writeableToSomeWriteable[T](writeable: Writeable[T]): Option[Writeable[T]] = Some(writeable)
  }
}

case class FacebookRequestBuilder(requests: ListBuffer[Request] = ListBuffer.empty) {

  import FacebookConnection._

  def get(relativeUrl: String, queryString: Map[String, Seq[String]], since: Option[Long], until: Option[Long], accessToken: Option[AccessToken]): this.type =
    batch(maybeRanged(since, until, GetRequest(relativeUrl, queryString, accessToken)))

  def post(relativeUrl: String, body: Option[String], queryString: Map[String, Seq[String]], since: Option[Long], until: Option[Long], accessToken: Option[AccessToken]): this.type =
    batch(maybeRanged(since, until, PostRequest(relativeUrl, queryString, accessToken, body)))

  def execute(implicit facebookConnection: FacebookConnection, accessToken: Option[AccessToken] = None, ec: ExecutionContext): Future[FacebookBatchResponse] =
    facebookConnection
      // assemble request parts and send it off
      .batch(makeParts(accessToken, requests))
      // map the response to our internal type
      .map(FacebookBatchResponse.fromWSResponse)

  def executeWithPagination(implicit facebookConnection: FacebookConnection, accessToken: Option[AccessToken] = None, ec: ExecutionContext): Future[Seq[(Request, FacebookBatchResponsePart)]] =
    _executeWithPagination(requests)

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
    case (req: RangedRequest, res) ⇒ (req.request, res) // original request so we can group all parts on it later
    case r                         ⇒ r
  }

  private def newRequestFromIncompleteRequest(reqRes: (Request, FacebookBatchResponsePart)): Request = {
    val paging = (reqRes._2.bodyJson \ "paging").validate[FacebookPagingInfo].get
    reqRes._1.asInstanceOf[RangedRequest].copy(currentSince = paging.nextSinceLong, currentUntil = paging.nextUntilLong)
  }

  private def isRequestComplete(reqRes: (Request, FacebookBatchResponsePart)): Boolean = {
    reqRes match {
      case (request: RangedRequest, response: FacebookBatchResponsePart) ⇒
        if (response.code == 200) {
          request.currentUntil.exists(_ >= request.until)
        } else {
          // error
          true
        }

      case (request: Request, response: FacebookBatchResponsePart) ⇒ true
    }
  }

  private def makeParts(accessToken: Option[AccessToken], requests: Seq[Request]): Seq[(String, Array[Byte])] = {
    accessToken.map { a ⇒ Seq(ACCESS_TOKEN -> a.token.getBytes("utf-8")) }
      .getOrElse(Seq.empty[(String, Array[Byte])]) ++
      Seq(BATCH -> ("[" + requests.map(_.toJson()).mkString(",") + "]").getBytes("utf-8"))
  }

  private def maybeRanged(since: Option[Long], until: Option[Long], request: Request): Request =
    if (since.isDefined && until.isDefined) RangedRequest(since.get, until.get, request)
    else request

  private def batch(request: Request): this.type = {
    requests += request
    this
  }
}

