package http.client.response

trait BatchResponse[P <: HttpResponse] extends HttpResponse {
  val parts: Seq[P]
}