package http.client.request

import http.client.connection.HttpConnection
import http.client.method.{GetMethod, HttpMethod}

abstract class GetRequest[C <: HttpConnection[D], D](relativeUrl: String, queryString: Map[String, Seq[String]], data: Option[D], method: HttpMethod = GetMethod) extends Request[D] {
  override val completionEvaluator = new TrueCompletionEvaluation

  override def toJson(extraQueryStringParams: Map[String, Seq[String]] = Map.empty): String
}
