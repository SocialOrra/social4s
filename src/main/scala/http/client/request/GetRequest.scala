package http.client.request

import http.client.method.{ GetMethod, HttpMethod }

abstract class GetRequest(relativeUrl: String, queryString: Map[String, Seq[String]], method: HttpMethod = GetMethod) extends Request {
  override val completionEvaluator = new TrueCompletionEvaluation
  override def toJson(extraQueryStringParams: Map[String, Seq[String]] = Map.empty): String
}
