package http.client.response

trait BatchResponse {
  val code: Int
  val headers: Map[String, Seq[String]]
  val parts: Seq[BatchResponsePart]
}
