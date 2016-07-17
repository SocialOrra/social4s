package twitter4s.request

import http.client.request.PaginatedHttpRequest
import http.client.response.HttpHeader

abstract class TwitterRequest extends PaginatedHttpRequest[TwitterRequest] {

  implicit val authHeaderGen: (TwitterRequest) ⇒ TwitterAuthorizationHeader
  val paginated: Boolean

  override def toJson(extraQueryStringParams: Map[String, Seq[String]]): String = "{}"

  protected def withoutHeader(httpHeaderName: String): TwitterRequest
}

object TwitterRequest {
  def newAuthHeaderForRequest(request: TwitterRequest): Seq[HttpHeader] = {

    // TODO: refactor the hardcoded string
    val authHeaderOpt = request.headers.find(_.name == "Authorization")

    authHeaderOpt map { authHeader ⇒
      // remove auth header
      val requestWithoutAuth = request.withoutHeader("Authorization")
      // create new auth header
      val authHeader = request.authHeaderGen(requestWithoutAuth)
      // add auth header to existing headers and return
      requestWithoutAuth.headers :+ authHeader
    } getOrElse Seq.empty
  }
}
