package facebook4s

import play.api.libs.json.{ JsObject, JsString }

trait Request {
  val method: HttpMethod
  val relativeUrl: String
  val queryString: Map[String, Seq[String]]
  val accessToken: Option[AccessToken]

  def toJson: String

  protected def queryStringAsStringWithToken = (queryString ++ accessToken.map(accessTokenQS))
    .flatMap { keyAndValues ⇒
      val key = keyAndValues._1
      keyAndValues._2.map(value ⇒ s"$key=$value").toList
    }
    .mkString("&")

  protected def accessTokenQS(accessToken: AccessToken): (String, Seq[String]) =
    FacebookConnection.ACCESS_TOKEN -> Seq(accessToken.token)

  protected def maybeQueryString: String = {
    if (queryString.nonEmpty) "?" + queryStringAsStringWithToken
    else ""
  }
}

trait HttpMethod { val name: String }
case object GetMethod extends HttpMethod { override val name = "GET" }
case object PostMethod extends HttpMethod { override val name = "POST" }

case class GetRequest(relativeUrl: String, queryString: Map[String, Seq[String]], accessToken: Option[AccessToken], method: HttpMethod = GetMethod) extends Request {
  override def toJson: String = {
    JsObject(Seq(
      "method" -> JsString(method.name),
      "relative_url" -> JsString(relativeUrl + maybeQueryString))).toString()
  }
}

case class PostRequest(relativeUrl: String, queryString: Map[String, Seq[String]], accessToken: Option[AccessToken], body: Option[String], method: HttpMethod = PostMethod) extends Request {

  override def toJson: String = {
    JsObject(Seq(
      "method" -> JsString(method.name),
      "relative_url" -> JsString(relativeUrl + maybeQueryString)) ++
      body.map(b ⇒ Seq("body" -> JsString(b))).getOrElse(Seq.empty) // TODO: URLEncode() the body string, needed?
      ).toString()
  }
}

