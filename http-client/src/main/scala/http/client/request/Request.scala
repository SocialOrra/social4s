package http.client.request

import java.io.UnsupportedEncodingException
import java.net.URLEncoder

import http.client.method.{ HttpMethod, PostMethod }
import http.client.response.BatchResponsePart

import scala.compat.Platform

trait Request {
  val method: HttpMethod
  val headers: Seq[(String, String)]
  val relativeUrl: String
  val queryString: Map[String, Seq[String]]
  val completionEvaluator: CompletionEvaluation
  def isComplete(response: BatchResponsePart): Boolean = completionEvaluator(this, response)
  def toJson(extraQueryStringParams: Map[String, Seq[String]] = Map.empty): String
}

