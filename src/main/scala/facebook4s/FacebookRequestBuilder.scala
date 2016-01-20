package facebook4s

import play.api.http.Writeable
import play.api.libs.json.{ JsError, JsSuccess }

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

  private def _executeWithPagination(requests: Seq[Request])(implicit facebookConnection: FacebookConnection, accessToken: Option[AccessToken] = None, ec: ExecutionContext): Future[Seq[(Request, FacebookBatchResponsePart)]] = {

    val parts = makeParts(accessToken, requests)

    facebookConnection.batch(parts).flatMap { rawResponse ⇒

      val response = FacebookBatchResponse.fromWSResponse(rawResponse)
      val executeNewRequests = _executeWithPagination(_: Seq[Request])(facebookConnection, accessToken, ec)

      val futures = requests
        // group up each request with it's corresponding response
        .zip(response.parts)
        // create two groups: complete, incomplete
        // note that  non-200 responses are considered complete
        .groupBy(isRequestComplete)
        // handle responses
        // if complete, accumulate
        // otherwise, rebuild new requests, fire them off, return parts
        .map(accumulateCompleteRequests orElse (newRequestsFromIncompleteRequests andThen executeNewRequests))

      // create a single future representing the sequence of futures
      Future.sequence(futures.toSeq).map(_.flatten)
    }
  }

  private def accumulateCompleteRequests: PartialFunction[(Boolean, Seq[(Request, FacebookBatchResponsePart)]), Future[Seq[(Request, FacebookBatchResponsePart)]]] = {
    case (true, complete) ⇒ Future.successful {
      complete.map {
        case (req: RangedRequest, res) ⇒ (req.request, res) // original request so we can group all parts on it later
        case reqRes                    ⇒ reqRes
      }
    }
  }

  private def newRequestsFromIncompleteRequests: PartialFunction[(Boolean, Seq[(Request, FacebookBatchResponsePart)]), Seq[Request]] = {
    case (false, incomplete) ⇒ incomplete map { reqRes ⇒
      paginateRequest(originalRequest = reqRes._1, response = reqRes._2)
    }
  }

  private def paginateRequest(originalRequest: Request, response: FacebookBatchResponsePart): RangedRequest = {
    val paging = (response.bodyJson \ "paging").validate[FacebookPagingInfo].get
    RangedRequest(since = paging.nextSinceLong.get, until = paging.nextUntilLong.get, originalRequest)
  }

  private def isRequestComplete(reqRes: (Request, FacebookBatchResponsePart)): Boolean = {
    reqRes match {
      case (request: RangedRequest, response: FacebookBatchResponsePart) ⇒
        if (response.code == 200) {
          (response.bodyJson \ "paging").validate[FacebookPagingInfo] match {
            case paging: JsSuccess[FacebookPagingInfo] ⇒
              // TODO: what if this never ends up finishing? time out outside? max attempts?
              paging.get.nextUntilLong.exists(_ >= request.until)
            case e: JsError ⇒
              // TODO: log
              true
          }
        } else {
          // TODO: how do we handle error responses?
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

