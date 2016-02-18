package facebook4s.request

import facebook4s.api.AccessToken
import facebook4s.connection.FacebookConnection
import facebook4s.response._
import http.client.method.{ PostMethod, GetMethod, HttpMethod }
import http.client.request._
import http.client.response.BatchResponsePart
import play.api.libs.json._
import play.api.libs.ws.WSResponse

import scala.collection.mutable.ListBuffer

object TimeRangeCompletionEvaluation extends CompletionEvaluation {
  override def apply(request: Request[_], response: BatchResponsePart): Boolean = {
    request.asInstanceOf[FacebookTimeRangedRequest].currentUntil.exists(_ >= request.asInstanceOf[FacebookTimeRangedRequest].until)
  }
}

case class Value(end_time: String)
case class Datum(values: Seq[Value])
case class Data(data: Seq[Datum])

object Value {
  implicit val fmt = Json.format[Value]
}
object Datum {
  implicit val fmt = Json.format[Datum]
}
object Data {
  implicit val fmt = Json.format[Data]
}

trait ConditionChecker[T] {
  def apply(e1: T, e2: T): Boolean
}

trait JsonExtractor[T] {
  def apply(json: JsValue, checker: ConditionChecker[T]): (T ⇒ Boolean)
}

class TrueChecker[T]() extends ConditionChecker[T] {
  override def apply(ignored: T, ignored2: T): Boolean = true
}

object JsonConditions {

  object data$values$end_time extends JsonExtractor[String] {

    def apply(jsonResponseBody: JsValue, checker: ConditionChecker[String]): (String ⇒ Boolean) = {
      jsonResponseBody
        .validate[Data]
        .asOpt
        .flatMap { d ⇒
          d.data
            .lastOption
            .flatMap(_.values.lastOption)
            .map(lastValue ⇒ checker.apply(lastValue.end_time, _: String))
        }.getOrElse(new TrueChecker[String].apply("", _)) // true if we failed to find what we're looking for
    }
  }
}

class JsonConditionCompletionEvaluation[T](jsonExtractor: JsonExtractor[T], checker: ConditionChecker[T], completionConditionValue: T) extends CompletionEvaluation {
  override def apply(request: Request[_], response: BatchResponsePart): Boolean =
    jsonExtractor.apply(response.bodyJson, checker).apply(completionConditionValue)
}

///// facebook api

object FacebookEmptyNextPageCompletionEvaluation extends CompletionEvaluation {
  override def apply(request: Request[_], response: BatchResponsePart): Boolean = {
    val paging = (response.bodyJson \ "paging").validate[FacebookCursorPaging].get
    paging.next.isEmpty
  }
}

object FacebookRequest {

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

case class FacebookGetRequest(val relativeUrl: String, val queryString: Map[String, Seq[String]], val data: Option[AccessToken], val method: HttpMethod = GetMethod)
    extends GetRequest[FacebookConnection, AccessToken](relativeUrl, queryString, data, method) {
  override def toJson(extraQueryStringParams: Map[String, Seq[String]] = Map.empty): String = {
    JsObject(Seq(
      "method" -> JsString(method.name),
      "relative_url" -> JsString(relativeUrl + FacebookRequest.maybeQueryString(queryString ++ extraQueryStringParams, data)))).toString()
  }
}

case class FacebookPostRequest(relativeUrl: String, queryString: Map[String, Seq[String]], data: Option[AccessToken], body: Option[String], method: HttpMethod = PostMethod)
    extends PostRequest[FacebookConnection, AccessToken](relativeUrl, queryString, data, body, method) {
  override def toJson(extraQueryStringParams: Map[String, Seq[String]] = Map.empty): String = {
    JsObject(Seq(
      "method" -> JsString(method.name),
      "relative_url" -> JsString(relativeUrl + FacebookRequest.maybeQueryString(queryString ++ extraQueryStringParams, data))) ++
      body.map(b ⇒ Seq("body" -> JsString(b))).getOrElse(Seq.empty) // TODO: URLEncode() the body string, needed?
      ).toString()
  }
}

trait FacebookPaginatedRequest extends Request[AccessToken] {
  def originalRequest: Request[AccessToken]
  def nextRequest(responsePart: BatchResponsePart): FacebookPaginatedRequest
}

case class FacebookTimeRangedRequest(since: Long, until: Long, request: Request[AccessToken], currentSince: Option[Long] = None, currentUntil: Option[Long] = None)
    extends FacebookPaginatedRequest {
  protected lazy val sinceUntil = Map("since" -> Seq(currentSince.getOrElse(since).toString), "until" -> Seq(currentUntil.getOrElse(until).toString))
  override val method = request.method
  override val relativeUrl = request.relativeUrl
  override val queryString = request.queryString ++ sinceUntil
  override val data = request.data
  override def originalRequest = copy(currentSince = None, currentUntil = None)
  override val completionEvaluator = TimeRangeCompletionEvaluation
  override def toJson(extraQueryStringParams: Map[String, Seq[String]] = Map.empty): String = request.toJson(extraQueryStringParams ++ sinceUntil)
  override def nextRequest(responsePart: BatchResponsePart): FacebookTimeRangedRequest = {
    val paging = (responsePart.bodyJson \ "paging").validate[FacebookTimePaging].get
    copy(currentSince = paging.nextSinceLong, currentUntil = paging.nextUntilLong)
  }
}

case class FacebookCursorPaginatedRequest(request: Request[AccessToken], paging: Option[FacebookCursorPaging] = None, completionEvaluator: CompletionEvaluation = FacebookEmptyNextPageCompletionEvaluation)
    extends FacebookPaginatedRequest {
  protected lazy val after = paging.map(p ⇒ Map("after" -> Seq(p.cursors.after))).getOrElse(Map.empty)
  override val method = request.method
  override val relativeUrl = request.relativeUrl
  override val queryString = request.queryString ++ after
  override val data = request.data
  override def originalRequest = copy(paging = None)
  override def toJson(extraQueryStringParams: Map[String, Seq[String]] = Map.empty): String = request.toJson(extraQueryStringParams ++ after)
  override def nextRequest(responsePart: BatchResponsePart): FacebookCursorPaginatedRequest = {
    val paging = (responsePart.bodyJson \ "paging").validate[FacebookCursorPaging].get
    copy(paging = Some(paging))
  }
}

class FacebookRequestBuilder(requests: ListBuffer[Request[AccessToken]] = ListBuffer.empty) extends HttpRequestBuilder[FacebookConnection, AccessToken]() {

  import FacebookConnection._

  override protected def maybeRanged(since: Option[Long], until: Option[Long], request: Request[AccessToken]): Request[AccessToken] =
    if (since.isDefined && until.isDefined) FacebookTimeRangedRequest(since.get, until.get, request)
    else request

  override protected def maybePaginated(paginated: Boolean, request: Request[AccessToken]): Request[AccessToken] =
    if (paginated) FacebookCursorPaginatedRequest(request)
    else request

  override protected def accumulateCompleteRequest(reqRes: (Request[AccessToken], BatchResponsePart)): (Request[AccessToken], BatchResponsePart) = reqRes match {
    case (req: FacebookPaginatedRequest, res) ⇒ (req.originalRequest, res) // original request so we can group all parts on it later
    case rr                                   ⇒ rr
  }

  override protected def newRequestFromIncompleteRequest(reqRes: (Request[AccessToken], BatchResponsePart)): Request[AccessToken] = {
    reqRes._1.asInstanceOf[FacebookPaginatedRequest].nextRequest(reqRes._2)
  }

  override protected def get(relativeUrl: String, queryString: Map[String, Seq[String]], data: Option[AccessToken]): Request[AccessToken] = FacebookGetRequest(relativeUrl, queryString, data)
  override protected def post(relativeUrl: String, queryString: Map[String, Seq[String]], data: Option[AccessToken], body: Option[String]): Request[AccessToken] = FacebookPostRequest(relativeUrl, queryString, data, body)

  override protected def makeParts(accessToken: Option[AccessToken], requests: Seq[Request[AccessToken]]): Seq[(String, Array[Byte])] = {
    accessToken.map { a ⇒ Seq(ACCESS_TOKEN -> a.token.getBytes("utf-8")) }
      .getOrElse(Seq.empty[(String, Array[Byte])]) ++
      Seq(BATCH -> ("[" + requests.map(_.toJson()).mkString(",") + "]").getBytes("utf-8"))
  }

  override protected def fromWSResponse(wsResponse: WSResponse): FacebookBatchResponse = {
    FacebookBatchResponse(wsResponse.status, wsResponse.allHeaders, wsResponse.json.validate[Seq[FacebookBatchResponsePart]].getOrElse(Seq.empty))
  }
}
