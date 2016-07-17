package google4s.request

import http.client.connection.HttpConnection
import http.client.response.HttpResponse
import org.slf4j.LoggerFactory

import scala.collection.mutable.ListBuffer
import scala.concurrent.{ExecutionContext, Future}

trait GoogleRequestBuilderCallback {
  def apply(completedRequest: HttpResponse): Future[Boolean]
}

class GoogleBatchRequestAccumulatorCallback extends GoogleRequestBuilderCallback {

  var completedRequests: ListBuffer[HttpResponse] = ListBuffer()

  def apply(completedRequest: HttpResponse): Future[Boolean] = Future.successful {
    completedRequests += completedRequest
    true
  }
}

class GoogleRequestBuilder(connection: HttpConnection) {

  protected var log = LoggerFactory.getLogger(getClass.getName)

  def shutdown() = connection.shutdown()

  def makeRequest[R <: GoogleRequest](request: GoogleRequest, partCompletionCallback: GoogleRequestBuilderCallback)(implicit ex: ExecutionContext): Future[Boolean] = {
    executeWithPagination(request, partCompletionCallback)
  }

  private def executeWithPagination[R <: GoogleRequest](request: GoogleRequest, partCompletionCallback: GoogleRequestBuilderCallback)(implicit ec: ExecutionContext): Future[Boolean] = {
    _executeWithPagination(request, partCompletionCallback)
  }

  private def _executeWithPagination[R <: GoogleRequest](
    request:                GoogleRequest,
    partCompletionCallback: GoogleRequestBuilderCallback,
    completedResponseParts: Seq[HttpResponse]            = Seq.empty)(
    implicit
    ec: ExecutionContext): Future[Boolean] = {

    val responseF = connection.makeRequest(request)

    responseF.map { response ⇒

      val newRequest = if (!isRequestComplete(request, response))
        Some(newRequestFromIncompleteRequest(request, response))
      else None

      (response, newRequest)

    } flatMap {
      case (responsePart, Some(newRequest)) ⇒
        partCompletionCallback(responsePart) flatMap {
          case true ⇒
            log.debug(s"Successfully called preCompletionCallback ${partCompletionCallback.getClass.getName} on response part.")
            _executeWithPagination(newRequest, partCompletionCallback, completedResponseParts ++ Seq(responsePart))
          case _ ⇒
            log.debug(s"Failed calling preCompletionCallback ${partCompletionCallback.getClass.getName} on parts, aborting.")
            Future.successful { false }
        }
      case (responsePart, None) ⇒
        partCompletionCallback(responsePart)
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

