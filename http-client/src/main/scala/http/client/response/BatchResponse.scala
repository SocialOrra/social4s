package http.client.response

trait BatchResponse[P <: HttpResponse] {
  val code: Int
  val headers: Seq[HttpHeader] //Map[String, Seq[String]]
  val parts: Seq[P]
}