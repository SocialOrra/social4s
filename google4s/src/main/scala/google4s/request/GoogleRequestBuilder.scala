package google4s.request

import http.client.connection.HttpConnection
import http.client.request.HttpRequestBuilder

class GoogleRequestBuilder(connection: HttpConnection) extends HttpRequestBuilder(connection)

