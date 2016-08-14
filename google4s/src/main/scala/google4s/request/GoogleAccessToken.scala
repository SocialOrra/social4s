package google4s.request

import akka.actor.ActorSystem
import http.client.connection.impl.{PlayWSHttpConnection, ThrottledHttpConnection}
import http.client.method.PostMethod
import http.client.request.{HttpRequestAccumulatorCallback, HttpRequestBuilderCallback}
import http.client.response.HttpHeader
import play.api.libs.json.{JsSuccess, Json}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.{Await, ExecutionContext, Future}

case class GoogleAccessTokenRenewResult(access_token: String, token_type: String, expires_in: Long)
object GoogleAccessTokenRenewResult {
  implicit val fmt = Json.format[GoogleAccessTokenRenewResult]
}

object GoogleAccessToken {

  val tokenBaseUrl = "https://accounts.google.com"
  val tokenRelativeUrl = "/o/oauth2/token"

  // TODO: document and remove hardcoded urls and strings
  def fromRenewToken(requestBuilder: GoogleRequestBuilder)(implicit clientSecret: String, clientId: String, refreshToken: String, ec: ExecutionContext): Future[Option[String]] = {

    val bodyParams = Map(
      "client_secret" → java.net.URLEncoder.encode(clientSecret, "utf-8"),
      "client_id" → java.net.URLEncoder.encode(clientId, "utf-8"),
      "refresh_token" → java.net.URLEncoder.encode(refreshToken, "utf-8"),
      "grant_type" → "refresh_token")
      .map { case (k, v) ⇒ s"$k=$v" }
      .mkString("&")

    val request = new GoogleRequest(
      baseUrl = tokenBaseUrl,
      relativeUrl = tokenRelativeUrl,
      method = PostMethod,
      headers = Seq(HttpHeader("content-type", "application/x-www-form-urlencoded")),
      accessToken = "",
      body = Some(bodyParams.getBytes("utf-8")),
      paginated = false)

    requestBuilder.makeRequest(request) map { response ⇒
      response.json.validate[GoogleAccessTokenRenewResult] match {
        case s: JsSuccess[GoogleAccessTokenRenewResult] ⇒ Some(s.get.access_token)
        case x ⇒
          println(s"Error renewing access token for clientId=$clientId\nresponse=$response\nbody=${response.body}\nerror=$x")
          None
      }
    }
  }

  def accessToken(requestBuilder: GoogleRequestBuilder, clientId: String, clientSecret: String, refreshToken: String)(implicit ec: ExecutionContext): Future[String] = {

    GoogleAccessToken
      .fromRenewToken(requestBuilder)(clientSecret, clientId, refreshToken, ec)
      .map { t ⇒
        t.getOrElse {
          println("No access token could be retrieved, expect failures.")
          "NO_ACCESS_TOKEN"
        }
      }
  }
}
