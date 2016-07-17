package twitter4s.request

import http.client.connection.HttpConnection
import http.client.request.HttpRequestBuilder

class TwitterRequestBuilder(connection: HttpConnection) extends HttpRequestBuilder(connection)

