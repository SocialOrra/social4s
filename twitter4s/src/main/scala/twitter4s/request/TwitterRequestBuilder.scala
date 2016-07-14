package twitter4s.request

import http.client.connection.HttpConnection
import http.client.response.HttpResponse

import scala.concurrent.{ExecutionContext, Future}

class TwitterRequestBuilder(connection: HttpConnection) {

  def shutdown() = connection.shutdown()

  def makeRequest[R <: TwitterRequest](request: TwitterRequest)(implicit ec: ExecutionContext, authHeaderGen: (TwitterRequest) ⇒ TwitterAuthorizationHeader): Future[(TwitterRequest, Seq[HttpResponse])] = {
    executeWithPagination(request)
  }

  def executeWithPagination[R <: TwitterRequest](request: TwitterRequest)(implicit ec: ExecutionContext, authHeaderGen: (TwitterRequest) ⇒ TwitterAuthorizationHeader): Future[(TwitterRequest, Seq[HttpResponse])] = {
    val f = _executeWithPagination(request) map { requestAndResponseParts ⇒
      // TODO: could there be no parts?
      val request = requestAndResponseParts._1
      val parts = requestAndResponseParts._2
      (request, parts)
    }
    f
  }

  private def _executeWithPagination[R <: TwitterRequest](
    request:                TwitterRequest,
    completedResponseParts: Seq[HttpResponse] = Seq.empty)(
    implicit
    ec:            ExecutionContext,
    authHeaderGen: (TwitterRequest) ⇒ TwitterAuthorizationHeader): Future[(TwitterRequest, Seq[HttpResponse])] = {

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

  private def isRequestComplete[R <: TwitterRequest](request: TwitterRequest, response: HttpResponse): Boolean = {

    if (response.status == 200) {
      request.isComplete(response)
    } else {
      // error
      true
    }
  }

  protected def newRequestFromIncompleteRequest[R <: TwitterRequest](request: TwitterRequest, response: HttpResponse)(implicit authHeaderGen: (TwitterRequest) ⇒ TwitterAuthorizationHeader): TwitterRequest = {
    request.nextRequest(response)
  }
}

