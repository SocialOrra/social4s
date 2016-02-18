package http.client.request

import http.client.connection.HttpConnection
import http.client.method.{HttpMethod, PostMethod}

abstract class PostRequest[C <: HttpConnection[D], D](relativeUrl: String, queryString: Map[String, Seq[String]], data: Option[D], body: Option[String], method: HttpMethod = PostMethod)
    extends Request[D] {
  override val completionEvaluator = new TrueCompletionEvaluation

  override def toJson(extraQueryStringParams: Map[String, Seq[String]] = Map.empty): String
}
