package http.client.request

import http.client.method.{HttpMethod, PostMethod}

//abstract class PostRequest2[T](val relativeUrl: String, val headers: Seq[(String, String)], val queryString: Map[String, Seq[String]], val body: Option[T], val method: HttpMethod = PostMethod) extends Request {
//  override val completionEvaluator = new TrueCompletionEvaluation
//  override def toJson(extraQueryStringParams: Map[String, Seq[String]] = Map.empty): String
//}
