package twitter4s

import http.client.connection.HttpConnection
import http.client.request.{GetRequest, PostRequest, Request}
import http.client.response.{BatchResponsePart, HttpResponse}
import play.api.http.Writeable

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.libs.json.JsValue

trait TwitterResponsePart extends BatchResponsePart

abstract class TwitterRequestBuilder(connection: HttpConnection, batchUrl: String) {

  protected def maybePaginated[T <: Request](paginated: Boolean, request: T): T
  protected def extractResponsePart(request: Request, respone: HttpResponse): TwitterResponsePart
  protected def newRequestFromIncompleteRequest(request: Request, response: TwitterResponsePart): Request
  protected def maybeRanged[T <: Request](since: Option[Long], until: Option[Long], request: T): T

  def shutdown() = connection.shutdown()

  def get(getRequest: GetRequest, since: Option[Long], until: Option[Long]): Future[(Request, Seq[TwitterResponsePart])] = {
    val r = maybeRanged(since, until, getRequest)
    val f = connection.get(r)
    executeWithPagination(r, f)
  }

  def get(getRequest: GetRequest, paginate: Boolean): Future[(Request, Seq[TwitterResponsePart])] = {
    val r = maybePaginated(paginate, getRequest)
    val f = connection.get(r)
    executeWithPagination(r, f)
  }

  def post[T](postRequest: PostRequest[T], since: Option[Long], until: Option[Long])(implicit writeable: Writeable[T]): Future[(Request, Seq[TwitterResponsePart])] = {
    val r = maybeRanged(since, until, postRequest)
    val f = connection.post(r)
    executeWithPagination(r, f)
  }

  def post[T](postRequest: PostRequest[T], paginated: Boolean)(implicit writeable: Writeable[T]): Future[(Request, Seq[TwitterResponsePart])] = {
    val r = maybePaginated(paginated, postRequest)
    val f = connection.post(r)
    executeWithPagination(r, f)
  }

  def executeWithPagination(request: Request, responseF: Future[HttpResponse])(implicit ec: ExecutionContext): Future[(Request, Seq[TwitterResponsePart])] = {
    val f = _executeWithPagination(request, responseF) map { requestAndResponseParts ⇒
      // TODO: could there be no parts?
      val request = requestAndResponseParts._1
      val parts = requestAndResponseParts._2
      (request, parts)
    }
    f
  }

  private def _executeWithPagination(request: Request, responseF: Future[HttpResponse], completedResponseParts: Seq[TwitterResponsePart] = Seq.empty)(implicit ec: ExecutionContext): Future[(Request, Seq[TwitterResponsePart])] = {

    responseF.map { response =>


      val responsePart = extractResponsePart(request, response)

      val newRequest = if (!isRequestComplete(request, responsePart))
        Some(newRequestFromIncompleteRequest(request, responsePart))
      else None

      (responsePart, newRequest)

    } flatMap {
      case (responsePart, Some(newRequest)) ⇒
        //_executeWithPagination(newRequest, completedResponseParts ++ Seq(responsePart))
        ???
      case (responsePart, None) ⇒
        Future.successful { (request, completedResponseParts ++ Seq(responsePart)) }
    }
  }

  private def isRequestComplete(request: Request, response: TwitterResponsePart): Boolean = {

    if (response.code== 200) {
      request.isComplete(response)
    } else {
      // error
      true
    }
  }
}

