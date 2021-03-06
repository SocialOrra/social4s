package http.client.request

import http.client.connection.HttpConnection
import http.client.response.{BatchResponse, HttpResponse}
import org.slf4j.LoggerFactory

import scala.collection.mutable.ListBuffer
import scala.concurrent.{ExecutionContext, Future}

trait HttpBatchRequestCallback[BResponsePart] {
  def apply(completedRequests: Seq[(HttpRequest, BResponsePart)]): Future[Boolean]
}

class HttpBatchRequestAccumulatorCallback[T] extends HttpBatchRequestCallback[T] {

  var completedRequests: ListBuffer[(HttpRequest, T)] = ListBuffer()

  def apply(completedRequest: Seq[(HttpRequest, T)]): Future[Boolean] = Future.successful {
    completedRequests ++= completedRequest
    true
  }
}

object HttpBatchRequestBuilder {
  def mergeResponseParts[BResponsePart <: HttpResponse](requestsAndResponses: Seq[(HttpRequest, BResponsePart)])(implicit ec: ExecutionContext): Map[HttpRequest, Seq[BResponsePart]] = {
    requestsAndResponses
      // group response parts by request
      .groupBy(_._1)
      // remove grouping key, leave (request,responseParts)
      .mapValues(_.map(_._2))
      .map { requestAndResponseParts ⇒
        // TODO: could there be no parts?
        val request = requestAndResponseParts._1
        val parts = requestAndResponseParts._2
        //val combinedBody: String = parts.map(p ⇒ p.bodyJson.validate[JsObject].get).foldLeft(JsObject(Seq.empty))(_ deepMerge _).toString()
        //val combinedPart = HttpResponse(code = parts.head.code, headers = parts.head.headers, body = combinedBody)
        (request, parts)
      }
  }
}

abstract class HttpBatchRequestBuilder[BResponse <: BatchResponse[BResponsePart], BResponsePart <: HttpResponse, BRequestBuilder <: HttpBatchRequestBuilder[BResponse, BResponsePart, BRequestBuilder]](var requests: ListBuffer[HttpRequest] = ListBuffer.empty[HttpRequest], connection: HttpConnection, batchUrl: String) {

  protected var log = LoggerFactory.getLogger(getClass.getName)

  protected def makeBatchRequestBody(requests: Seq[HttpRequest]): Array[Byte]
  protected def makeBatchRequest(batchUrl: String, body: Array[Byte]): HttpRequest
  protected def fromHttpResponse(response: HttpResponse): BResponse
  protected def accumulateCompleteRequest(reqRes: (HttpRequest, BResponsePart)): (HttpRequest, BResponsePart)
  protected def newRequestFromIncompleteRequest(reqRes: (HttpRequest, BResponsePart)): HttpRequest
  protected def maybePaginated(paginated: Boolean, request: HttpRequest): HttpRequest
  protected def maybeRanged(since: Option[Long], until: Option[Long], request: HttpRequest): HttpRequest

  def shutdown() = connection.shutdown()

  def add(request: HttpRequest, since: Option[Long], until: Option[Long]): Unit =
    batch(maybeRanged(since, until, request))

  def add(request: HttpRequest, paginated: Boolean): Unit =
    batch(maybePaginated(paginated, request))

  def execute(implicit ec: ExecutionContext): Future[BResponse] = {
    // assemble request parts
    val body = makeBatchRequestBody(requests)
    val request = makeBatchRequest(batchUrl, body)
    // and send it off
    val f = connection.makeRequest(request)
      // map the response to our internal type
      .map(fromHttpResponse)

    postExecute()
    f
  }

  def executeWithPagination(partialCompletionCallback: HttpBatchRequestCallback[BResponsePart])(implicit ec: ExecutionContext): Future[Boolean] = {
    val f = _executeWithPagination(requests, partialCompletionCallback)
    postExecute()
    f
  }

  private def postExecute(): Unit = {
    requests = ListBuffer.empty
  }

  private def _executeWithPagination(requests: Seq[HttpRequest], partCompletionCallback: HttpBatchRequestCallback[BResponsePart])(implicit ec: ExecutionContext): Future[Boolean] = {

    val body = makeBatchRequestBody(requests)
    val postRequest = makeBatchRequest(batchUrl, body)

    connection.makeRequest(postRequest).map { rawResponse ⇒

      val response = fromHttpResponse(rawResponse)

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
        partCompletionCallback(complete) flatMap {
          case true ⇒
            log.debug(s"Successfully called preCompletionCallback ${partCompletionCallback.getClass.getName} on ${complete.size} parts.")
            _executeWithPagination(incomplete, partCompletionCallback)
          case _ ⇒
            log.debug(s"Failed calling preCompletionCallback ${partCompletionCallback.getClass.getName} on ${complete.size} parts, aborting.")
            Future.successful { false }
        }

      case (complete, _) ⇒
        partCompletionCallback(complete)
    }
  }

  private def isRequestComplete(reqRes: (HttpRequest, BResponsePart)): Boolean = {
    val request = reqRes._1
    val response = reqRes._2

    if (response.status == 200) {
      log.info(s"Checking if request is complete: ${request.isComplete(reqRes._2)} for ${reqRes._1.relativeUrl}")
      request.isComplete(reqRes._2)
    } else {
      log.info("Response status != 200, not checking if it's complete, simply returning true.")
      // error
      true
    }
  }

  private def batch(request: HttpRequest): Unit = {
    requests += request
  }
}

