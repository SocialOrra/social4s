package facebook4s.connection

case class FacebookConnectionInformation(
  graphApiHost: String = "graph.facebook.com",
  protocol:     String = "https",
  version:      String = "v2.5"
)
