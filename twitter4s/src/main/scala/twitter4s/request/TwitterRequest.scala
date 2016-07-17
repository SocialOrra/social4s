package twitter4s.request

import http.client.request.HttpRequest
import http.client.response.{HttpHeader, HttpResponse}

abstract class TwitterRequest extends HttpRequest {

  val paginated: Boolean

  override def toJson(extraQueryStringParams: Map[String, Seq[String]]): String = "{}"

  def nextRequest(response: HttpResponse)(implicit authHeaderGen: (TwitterRequest) ⇒ TwitterAuthorizationHeader): TwitterRequest

  protected def withoutHeader(httpHeaderName: String): TwitterRequest
}

object TwitterRequest {
  def newAuthHeaderForRequest(request: TwitterRequest)(implicit authHeaderGen: (TwitterRequest) ⇒ TwitterAuthorizationHeader): Seq[HttpHeader] = {

    // TODO: refactor the hardcoded string
    val authHeaderOpt = request.headers.find(_.name == "Authorization")

    authHeaderOpt map { authHeader ⇒
      // remove auth header
      val requestWithoutAuth = request.withoutHeader("Authorization")
      // create new auth header
      val authHeader = authHeaderGen(requestWithoutAuth)
      // add auth header to existing headers and return
      requestWithoutAuth.headers :+ authHeader
    } getOrElse Seq.empty
  }
}
