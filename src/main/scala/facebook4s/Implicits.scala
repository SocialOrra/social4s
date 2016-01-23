package facebook4s

import facebook4s.request.FacebookRequestBuilder
import facebook4s.response.FacebookBatchResponse

object Implicits
  extends FacebookBatchResponse.Implicits
  with FacebookRequestBuilder.Implicits
