import sbt._

object Dependencies {
  val logback        = "ch.qos.logback" % "logback-classic" % "1.1.1"
  val play           = "com.typesafe.play" %% "play" % "2.4.6" % "provided"
  val playJson       = "com.typesafe.play" %% "play-json" % "2.4.6"
  val playWs         = "com.typesafe.play" %% "play-ws" % "2.4.6"
  val scalaTest      = "org.scalatest" %% "scalatest" % "2.2.1" % "test"
  val scalaTestPlus  = "org.scalatestplus" %% "play" % "1.4.0-M3" % "test"
  val typesafeConfig = "com.typesafe" % "config" % "1.2.0"
}

