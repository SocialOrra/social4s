package http.client.response

trait BatchResponse[P <: BatchResponsePart] {
  val code: Int
  val headers: Map[String, Seq[String]]
  val parts: Seq[P]
}
