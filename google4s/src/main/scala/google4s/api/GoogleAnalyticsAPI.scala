package google4s.api

import google4s.request.{GoogleRequest, GoogleRequestBuilder}
import http.client.method.GetMethod
import http.client.request.{HttpRequestBuilderCallback, HttpRequestHelpers}

import scala.concurrent.ExecutionContext

object GoogleAnalyticsAPI extends HttpRequestHelpers {

  implicit class GoogleAnalyticsApiImplicits(requestBuilder: GoogleRequestBuilder) {
    def metrics(
      ids:         String,
      dimensions:  String,
      metrics:     String,
      startDate:   String,
      endDate:     String,
      accessToken: String)(implicit partCompletionCallback: HttpRequestBuilderCallback, ec: ExecutionContext) = {

      val request = new GoogleRequest(
        baseUrl = "https://www.googleapis.com/analytics/v3",
        relativeUrl = "/data/ga",
        method = GetMethod,
        accessToken = accessToken,
        paginated = true,
        _queryString = Map(
          "ids" → Seq(ids),
          "start-date" → Seq(startDate),
          "end-date" → Seq(endDate),
          "metrics" → Seq(metrics),
          "dimensions" → Seq(dimensions))) //Seq("ga:socialNetwork"),
      //"max-results" → Seq("1")))

      requestBuilder.makeRequest(request, partCompletionCallback)
    }
  }
}
