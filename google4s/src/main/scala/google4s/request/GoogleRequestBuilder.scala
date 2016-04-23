package google4s.request

import http.client.connection.HttpConnection
import http.client.request.Request
import http.client.response.HttpResponse

import scala.concurrent.{ExecutionContext, Future}

class GoogleRequestBuilder(connection: HttpConnection) {

  def shutdown() = connection.shutdown()

  def makeRequest[R <: GoogleRequest](request: GoogleRequest)(implicit ex: ExecutionContext): Future[(Request, Seq[HttpResponse])] = {
    executeWithPagination(request)
  }

  private def executeWithPagination[R <: GoogleRequest](request: GoogleRequest)(implicit ec: ExecutionContext): Future[(Request, Seq[HttpResponse])] = {
    val f = _executeWithPagination(request) map { requestAndResponseParts ⇒
      // TODO: could there be no parts?
      val request = requestAndResponseParts._1
      val parts = requestAndResponseParts._2
      (request, parts)
    }
    f
  }

  private def _executeWithPagination[R <: GoogleRequest](
    request:                GoogleRequest,
    completedResponseParts: Seq[HttpResponse] = Seq.empty)(
    implicit
    ec: ExecutionContext): Future[(GoogleRequest, Seq[HttpResponse])] = {

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

  private def isRequestComplete[R <: GoogleRequest](request: GoogleRequest, response: HttpResponse): Boolean = {

    if (response.status == 200) {
      request.isComplete(response)
    } else {
      // error
      true
    }
  }

  protected def newRequestFromIncompleteRequest[R <: GoogleRequest](request: GoogleRequest, response: HttpResponse): GoogleRequest = {
    request.nextRequest(response)
  }
}
