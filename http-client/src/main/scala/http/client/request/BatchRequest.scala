package http.client.request

case class BatchRequest(url: String, parts: Seq[(String, Array[Byte])])
