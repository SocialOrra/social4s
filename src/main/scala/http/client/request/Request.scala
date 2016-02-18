package http.client.request

import http.client.method.HttpMethod
import http.client.response.BatchResponsePart

trait Request[D] {
  val method: HttpMethod
  val relativeUrl: String
  val queryString: Map[String, Seq[String]]
  val data: Option[D]
  val completionEvaluator: CompletionEvaluation
  def isComplete(response: BatchResponsePart): Boolean = completionEvaluator(this, response)
  def toJson(extraQueryStringParams: Map[String, Seq[String]] = Map.empty): String
}
