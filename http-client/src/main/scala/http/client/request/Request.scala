package http.client.request

import http.client.method.HttpMethod
import http.client.response.HttpResponse

trait Request {
  val method: HttpMethod
  val headers: Seq[(String, String)]
  val body: Option[Array[Byte]]
  val relativeUrl: String
  val queryString: Map[String, Seq[String]]
  val completionEvaluator: CompletionEvaluation
  def isComplete(response: HttpResponse): Boolean = completionEvaluator(this, response)
  def toJson(extraQueryStringParams: Map[String, Seq[String]] = Map.empty): String
}

