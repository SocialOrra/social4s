package google4s.request

import http.client.connection.HttpConnection
import http.client.method.PostMethod
import http.client.request.Request
import http.client.response.{HttpHeader, HttpResponse}

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

object GoogleAccessToken {

  def renew(requestBuilder: GoogleRequestBuilder)(implicit clientSecret: String, clientId: String, refreshToken: String, ec: ExecutionContext): Future[HttpResponse] = {

    val bodyParams = Map(
      "client_secret" → java.net.URLEncoder.encode(clientSecret, "utf-8"),
      "client_id" → java.net.URLEncoder.encode(clientId, "utf-8"),
      "refresh_token" → java.net.URLEncoder.encode(refreshToken, "utf-8"),
      "grant_type" → "refresh_token")
      .map { case (k, v) ⇒ s"$k=$v" }
      .mkString("&")

    println(s"body=$bodyParams")

    val request = new GoogleRequest(
      relativeUrl = "https://accounts.google.com/o/oauth2/token",
      method = PostMethod,
      headers = Seq(HttpHeader("content-type", "application/x-www-form-urlencoded")),
      _queryString = Map.empty,
      //Map(
      //  "client_secret" → Seq(java.net.URLEncoder.encode(clientSecret, "utf-8")),
      //  "client_id" → Seq(java.net.URLEncoder.encode(clientId, "utf-8")),
      //  "refresh_token" → Seq(java.net.URLEncoder.encode(refreshToken, "utf-8")),
      //  "grant_type" → Seq("refresh_token")),
      accessToken = "",
      body = Some(bodyParams.getBytes("utf-8")),
      paginated = false)

    requestBuilder.makeRequest(request) map {
      case (req, response) ⇒
        response.head
    }
  }
}