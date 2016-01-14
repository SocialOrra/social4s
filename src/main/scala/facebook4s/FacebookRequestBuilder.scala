package facebook4s

import play.api.http.Writeable
import play.api.libs.ws.WSResponse

import scala.collection.mutable.ListBuffer
import scala.concurrent.Future

object FacebookRequestBuilder {

  trait Implicits {
    implicit def writeableToSomeWriteable[T](writeable: Writeable[T]): Option[Writeable[T]] = Some(writeable)
  }
}

case class FacebookRequestBuilder(var requests: ListBuffer[Request] = ListBuffer.empty) {

  import FacebookConnection._

  def get(relativeUrl: String, queryString: Map[String, Seq[String]], accessToken: Option[AccessToken]): this.type =
    batch(GetRequest(relativeUrl, queryString, accessToken))

  def post(relativeUrl: String, body: Option[String], queryString: Map[String, Seq[String]], accessToken: Option[AccessToken]): this.type =
    batch(PostRequest(relativeUrl, queryString, accessToken, body))

  def execute(implicit facebookConnection: FacebookConnection, accessToken: Option[AccessToken] = None): Future[WSResponse] = {

    val accessTokenPart = accessToken.map { a ⇒
      Seq(ACCESS_TOKEN -> a.token.getBytes("utf-8"))
    }.getOrElse(Seq.empty[(String, Array[Byte])])

    val batchPart = BATCH -> ("[" + requests.map(_.toJson).mkString(",") + "]").getBytes("utf-8")

    facebookConnection.batch(accessTokenPart :+ batchPart)
  }

  private def batch(request: Request): this.type = {
    requests += request
    this
  }
}

