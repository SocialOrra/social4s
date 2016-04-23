import sbt._

object Dependencies {
  val bucket4j        = "com.github" % "bucket4j" % "1.0.1"
  val googleAnalytics = "com.google.apis" % "google-api-services-analytics" % "v3-rev98-1.19.0"
  val googleOAuth     = "com.google.oauth-client" % "google-oauth-client-jetty" % "1.19.0"
  val googleApiClient = "com.google.api-client" % "google-api-client" % "1.20.0"
  val logback         = "ch.qos.logback" % "logback-classic" % "1.1.1"
  val play            = "com.typesafe.play" %% "play" % "2.4.6" % "provided"
  val playJson        = "com.typesafe.play" %% "play-json" % "2.4.6"
  val playWs          = "com.typesafe.play" %% "play-ws" % "2.4.6"
  val scalaTest       = "org.scalatest" %% "scalatest" % "2.2.1" % "test"
  val scalaTestPlus   = "org.scalatestplus" %% "play" % "1.4.0" % "test"
  val typesafeConfig  = "com.typesafe" % "config" % "1.2.0"
}

