package twitter4s.request

import http.client.connection.HttpConnection
import http.client.response.HttpResponse
import org.slf4j.LoggerFactory

import scala.collection.mutable.ListBuffer
import scala.concurrent.{ExecutionContext, Future}

trait TwitterRequestBuilderCallback {
  def apply(completedRequest: HttpResponse): Future[Boolean]
}

class TwitterBatchRequestAccumulatorCallback extends TwitterRequestBuilderCallback {

  var completedRequests: ListBuffer[HttpResponse] = ListBuffer()

  def apply(completedRequest: HttpResponse): Future[Boolean] = Future.successful {
    completedRequests += completedRequest
    true
  }
}

class TwitterRequestBuilder(connection: HttpConnection) {

  protected var log = LoggerFactory.getLogger(getClass.getName)

  def shutdown() = connection.shutdown()

  def makeRequest[R <: TwitterRequest](request: TwitterRequest, partCompletionCallback: TwitterRequestBuilderCallback)(implicit ec: ExecutionContext, authHeaderGen: (TwitterRequest) ⇒ TwitterAuthorizationHeader): Future[Boolean] = {
    executeWithPagination(request, partCompletionCallback)
  }

  protected def executeWithPagination[R <: TwitterRequest](request: TwitterRequest, partCompletionCallback: TwitterRequestBuilderCallback)(implicit ec: ExecutionContext, authHeaderGen: (TwitterRequest) ⇒ TwitterAuthorizationHeader): Future[Boolean] = {
    _executeWithPagination(request, partCompletionCallback)
  }

  private def _executeWithPagination[R <: TwitterRequest](
    request:                TwitterRequest,
    partCompletionCallback: TwitterRequestBuilderCallback,
    completedResponseParts: Seq[HttpResponse]             = Seq.empty)(
    implicit
    ec:            ExecutionContext,
    authHeaderGen: (TwitterRequest) ⇒ TwitterAuthorizationHeader): Future[Boolean] = {

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

