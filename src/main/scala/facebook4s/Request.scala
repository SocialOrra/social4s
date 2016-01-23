package facebook4s

import play.api.libs.json.{ JsObject, JsString }

trait Request {
  val method: HttpMethod
  val relativeUrl: String
  val queryString: Map[String, Seq[String]]
  val accessToken: Option[AccessToken]

  def toJson(extraQueryStringParams: Map[String, Seq[String]] = Map.empty): String
}

object Request {

  def queryStringAsStringWithToken(queryString: Map[String, Seq[String]], accessToken: Option[AccessToken]) =
    (queryString ++ accessToken.map(accessTokenQS))
      .flatMap { keyAndValues ⇒
        val key = keyAndValues._1
        keyAndValues._2.map(value ⇒ s"$key=$value").toList
      }
      .mkString("&")

  def accessTokenQS(accessToken: AccessToken): (String, Seq[String]) =
    FacebookConnection.ACCESS_TOKEN -> Seq(accessToken.token)

  def maybeQueryString(queryString: Map[String, Seq[String]], accessToken: Option[AccessToken]): String = {
    if (queryString.nonEmpty) "?" + queryStringAsStringWithToken(queryString, accessToken)
    else ""
  }
}

trait HttpMethod { val name: String }
case object GetMethod extends HttpMethod { override val name = "GET" }
case object PostMethod extends HttpMethod { override val name = "POST" }

case class GetRequest(relativeUrl: String, queryString: Map[String, Seq[String]], accessToken: Option[AccessToken], method: HttpMethod = GetMethod) extends Request {
  override def toJson(extraQueryStringParams: Map[String, Seq[String]] = Map.empty): String = {
    JsObject(Seq(
      "method" -> JsString(method.name),
      "relative_url" -> JsString(relativeUrl + Request.maybeQueryString(queryString ++ extraQueryStringParams, accessToken)))).toString()
  }
}

case class PostRequest(relativeUrl: String, queryString: Map[String, Seq[String]], accessToken: Option[AccessToken], body: Option[String], method: HttpMethod = PostMethod) extends Request {

  override def toJson(extraQueryStringParams: Map[String, Seq[String]] = Map.empty): String = {
    JsObject(Seq(
      "method" -> JsString(method.name),
      "relative_url" -> JsString(relativeUrl + Request.maybeQueryString(queryString ++ extraQueryStringParams, accessToken))) ++
      body.map(b ⇒ Seq("body" -> JsString(b))).getOrElse(Seq.empty) // TODO: URLEncode() the body string, needed?
      ).toString()
  }
}

case class RangedRequest(since: Long, until: Long, request: Request, currentSince: Option[Long] = None, currentUntil: Option[Long] = None) extends Request {
  protected val sinceUntil = Map("since" -> Seq(currentSince.getOrElse(since).toString), "until" -> Seq(currentUntil.getOrElse(until).toString))
  override val method = request.method
  override val relativeUrl = request.relativeUrl
  override val queryString = request.queryString ++ sinceUntil
  override val accessToken = request.accessToken
  override def toJson(extraQueryStringParams: Map[String, Seq[String]] = Map.empty): String = request.toJson(extraQueryStringParams ++ sinceUntil)
}
