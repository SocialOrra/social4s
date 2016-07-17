package http.client.request

import http.client.connection.HttpConnection
import http.client.response.HttpResponse
import org.slf4j.LoggerFactory

import scala.collection.mutable.ListBuffer
import scala.concurrent.{ExecutionContext, Future}

trait HttpRequestBuilderCallback {
  def apply(completedRequest: HttpResponse): Future[Boolean]
}

class HttpRequestAccumulatorCallback extends HttpRequestBuilderCallback {

  var completedRequests: ListBuffer[HttpResponse] = ListBuffer()

  def apply(completedRequest: HttpResponse): Future[Boolean] = Future.successful {
    completedRequests += completedRequest
    true
  }
}

class HttpRequestBuilder(connection: HttpConnection) {

  protected var log = LoggerFactory.getLogger(getClass.getName)

  def shutdown() = connection.shutdown()

  def makeRequest[R <: HttpRequest](request: R)(implicit ec: ExecutionContext): Future[HttpResponse] = {
    connection.makeRequest(request)
  }

  def makeRequest(request: PaginatedHttpRequest[_], partCompletionCallback: HttpRequestBuilderCallback)(implicit ec: ExecutionContext): Future[Boolean] = {
    executeWithPagination(request, partCompletionCallback)
  }

  protected def executeWithPagination(request: PaginatedHttpRequest[_], partCompletionCallback: HttpRequestBuilderCallback)(implicit ec: ExecutionContext): Future[Boolean] = {
    _executeWithPagination(request, partCompletionCallback)
  }

  private def _executeWithPagination(
    request:                PaginatedHttpRequest[_],
    partCompletionCallback: HttpRequestBuilderCallback,
    completedResponseParts: Seq[HttpResponse]          = Seq.empty)(
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

  private def isRequestComplete(request: PaginatedHttpRequest[_], response: HttpResponse): Boolean = {

    if (response.status == 200) {
      request.isComplete(response)
    } else {
      // error
      true
    }
  }

  protected def newRequestFromIncompleteRequest(request: PaginatedHttpRequest[_], response: HttpResponse): PaginatedHttpRequest[_] = {
    request.nextRequest(response)
  }
}

