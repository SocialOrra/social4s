package http.client.request

import http.client.method.{ HttpMethod, PostMethod }

abstract class PostRequest[T](relativeUrl: String, queryString: Map[String, Seq[String]], val body: Option[T], method: HttpMethod = PostMethod) extends Request {
  override val completionEvaluator = new TrueCompletionEvaluation
  override def toJson(extraQueryStringParams: Map[String, Seq[String]] = Map.empty): String
}
