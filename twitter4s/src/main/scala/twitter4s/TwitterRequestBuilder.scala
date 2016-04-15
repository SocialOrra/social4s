package twitter4s

import http.client.connection.HttpConnection
import http.client.request.Request
import http.client.response.HttpResponse

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.ExecutionContext.Implicits.global

class TwitterRequestBuilder(connection: HttpConnection) {

  def shutdown() = connection.shutdown()

  def makeRequest(request: Request, since: Option[Long], until: Option[Long]): Future[(Request, Seq[HttpResponse])] = {
    val r = maybeRanged(since, until, request)
    executeWithPagination(r)
  }

  def makeRequest(request: Request, paginated: Boolean): Future[(Request, Seq[HttpResponse])] = {
    val r = maybePaginated(paginated, request)
    executeWithPagination(r)
  }

  def executeWithPagination(request: Request)(implicit ec: ExecutionContext): Future[(Request, Seq[HttpResponse])] = {
    val f = _executeWithPagination(request) map { requestAndResponseParts ⇒
      // TODO: could there be no parts?
      val request = requestAndResponseParts._1
      val parts = requestAndResponseParts._2
      (request, parts)
    }
    f
  }

  private def _executeWithPagination(request: Request, completedResponseParts: Seq[HttpResponse] = Seq.empty)(implicit ec: ExecutionContext): Future[(Request, Seq[HttpResponse])] = {

    val responseF = connection.makeRequest(request)

    responseF.map { response ⇒

      val newRequest = if (!isRequestComplete(request, response))
        Some(newRequestFromIncompleteRequest(request, response))
      else None

      (response, newRequest)

    } flatMap {
      case (responsePart, Some(newRequest)) ⇒
        _executeWithPagination(newRequest, completedResponseParts ++ Seq(responsePart))
      case (responsePart, None) ⇒
        Future.successful { (request, completedResponseParts ++ Seq(responsePart)) }
    }
  }

  private def isRequestComplete(request: Request, response: HttpResponse): Boolean = {

    if (response.status == 200) {
      //request.isComplete(response)
      // TODO: implement
      true
    } else {
      // error
      true
    }
  }

  protected def newRequestFromIncompleteRequest(request: Request, response: HttpResponse): Request = ???
  protected def maybeRanged(since: Option[Long], until: Option[Long], request: Request): Request = request // TODO: implement
  protected def maybePaginated(paginated: Boolean, request: Request): Request = request // TODO: implement
}

