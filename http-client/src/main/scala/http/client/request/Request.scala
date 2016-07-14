package http.client.request

import http.client.method.HttpMethod
import http.client.response.{HttpHeader, HttpResponse}

trait Request {
  val baseUrl: String
  val method: HttpMethod
  val headers: Seq[HttpHeader]
  val body: Option[Array[Byte]]
  val relativeUrl: String
  val queryString: Map[String, Seq[String]]
  val completionEvaluator: CompletionEvaluation
  def isComplete(response: HttpResponse): Boolean = completionEvaluator(this, response)

  // TODO: get rid of this toJson method, it's an artifact of Facebook
  def toJson(extraQueryStringParams: Map[String, Seq[String]] = Map.empty): String
}

