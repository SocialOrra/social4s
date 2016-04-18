package twitter4s

import http.client.connection.HttpConnection
import http.client.method.HttpMethod
import http.client.request.{CompletionEvaluation, Request, TrueCompletionEvaluation}
import http.client.response.{HttpHeader, HttpResponse}
import play.api.libs.json.{Json, JsSuccess}

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.ExecutionContext.Implicits.global

/** Keeps scrolling so long as the next_cursor is greater than zero.
 */
object TwitterEmptyNextCursorCompletionEvaluation extends CompletionEvaluation {
  override def apply(request: Request, response: HttpResponse): Boolean = {
    (response.json \ "next_cursor").validate[Long] match {
      case s: JsSuccess[Long] if s.get > 0L ⇒ false
      case _                                ⇒ true
    }
  }
}

object TwitterEmptyResponseBodyCompletionEvaluation extends CompletionEvaluation {
  override def apply(request: Request, response: HttpResponse): Boolean = {
    response.body.isEmpty
  }
}

object TwitterTimelineRequest {
  case class DataId(id: Long)
  implicit val dataIdFmt = Json.format[DataId]
}

case class TwitterTimelineRequest(relativeUrl: String, headers: Seq[HttpHeader], queryString: Map[String, Seq[String]], body: Option[Array[Byte]], method: HttpMethod, paginated: Boolean, customCompletionEvaluator: Option[CompletionEvaluation] = None) extends TwitterRequest {

  override val completionEvaluator = if (paginated) {
    customCompletionEvaluator match {
      case Some(c) ⇒ new CompletionEvaluation {
        override def apply(v1: Request, v2: HttpResponse): Boolean = {
          c.apply(v1, v2) || TwitterEmptyResponseBodyCompletionEvaluation.apply(v1, v2)
        }
      }

      case _ ⇒ TwitterEmptyResponseBodyCompletionEvaluation
    }
  } else TrueCompletionEvaluation

  override def nextRequest(response: HttpResponse): TwitterRequest = {
    // take last item in data, take it's ID, subtract 1 from it, and set it as max_id.
    response.json.validate[Array[TwitterTimelineRequest.DataId]] match {
      case s: JsSuccess[Array[TwitterTimelineRequest.DataId]] ⇒
        // we subtract 1 in order not to re-include the last item in the timeline
        val newQS = queryString + ("max_id" → Seq((s.get.last.id - 1).toString))
        copy(queryString = newQS)
      case _ ⇒
        // TODO: how do we handle this case?
        println(s"OH NO! Could not find data with an id in ${response.json.toString}")
        ???
    }
  }
}

case class TwitterCursoredRequest(relativeUrl: String, headers: Seq[HttpHeader], queryString: Map[String, Seq[String]], body: Option[Array[Byte]], method: HttpMethod, paginated: Boolean) extends TwitterRequest {

  override val completionEvaluator = if (paginated) TwitterEmptyNextCursorCompletionEvaluation else TrueCompletionEvaluation

  override def nextRequest(response: HttpResponse): TwitterRequest = {
    val next = (response.json \ "next_cursor").validate[Long].get
    val newQS = queryString + ("cursor" → Seq(next.toString))
    copy(queryString = newQS)
  }
}

abstract class TwitterRequest extends Request {

  val paginated: Boolean

  override def toJson(extraQueryStringParams: Map[String, Seq[String]]): String = "{}"

  def nextRequest(response: HttpResponse): TwitterRequest
}

class TwitterRequestBuilder(connection: HttpConnection) {

  def shutdown() = connection.shutdown()

  def makeRequest[R <: TwitterRequest](request: TwitterRequest, since: Option[Long], until: Option[Long]): Future[(Request, Seq[HttpResponse])] = {
    //val r = maybeRanged(since, until, request)
    executeWithPagination(request)
  }

  def makeRequest[R <: TwitterRequest](request: TwitterRequest): Future[(Request, Seq[HttpResponse])] = {
    executeWithPagination(request)
  }

  def executeWithPagination[R <: TwitterRequest](request: TwitterRequest)(implicit ec: ExecutionContext): Future[(Request, Seq[HttpResponse])] = {
    val f = _executeWithPagination(request) map { requestAndResponseParts ⇒
      // TODO: could there be no parts?
      val request = requestAndResponseParts._1
      val parts = requestAndResponseParts._2
      (request, parts)
    }
    f
  }

  private def _executeWithPagination[R <: TwitterRequest](request: TwitterRequest, completedResponseParts: Seq[HttpResponse] = Seq.empty)(implicit ec: ExecutionContext): Future[(TwitterRequest, Seq[HttpResponse])] = {

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

  private def isRequestComplete[R <: TwitterRequest](request: TwitterRequest, response: HttpResponse): Boolean = {

    if (response.status == 200) {
      // this only works for paginated requests with cursors where we want to scroll until the end
      // TODO: need to support since / until as well, so this code will move elsewhere
      //       since / until can be implemented via a custom completion evaluator that takes user input into account
      request.isComplete(response)
    } else {
      // error
      true
    }
  }

  protected def newRequestFromIncompleteRequest[R <: TwitterRequest](request: TwitterRequest, response: HttpResponse): TwitterRequest = {
    request.nextRequest(response)
  }
}

