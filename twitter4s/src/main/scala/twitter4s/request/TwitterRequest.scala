package twitter4s.request

import http.client.request.Request
import http.client.response.HttpResponse

abstract class TwitterRequest extends Request {

  val paginated: Boolean

  override def toJson(extraQueryStringParams: Map[String, Seq[String]]): String = "{}"

  def nextRequest(response: HttpResponse): TwitterRequest
}
