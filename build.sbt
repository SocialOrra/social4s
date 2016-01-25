import sbt._
import Dependencies._

lazy val commonSettings = Seq(
  name := "facebook4s",
  version := "1.0.0",
  organization := "facebook4s",
  scalaVersion := "2.11.7",
  resolvers ++= Seq(Resolver.mavenLocal,
    "Sonatype OSS Releases" at "http://oss.sonatype.org/content/repositories/releases/",
    "Sonatype OSS Snapshots" at "http://oss.sonatype.org/content/repositories/snapshots/",
    "Typesafe Snapshots" at "http://repo.typesafe.com/typesafe/snapshots/",
    "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/")
)

lazy val f4sDeps = Seq(
  typesafeConfig,
  playJson,
  playWs,
  scalaTest,
  scalaTestPlus
)

lazy val f4s = (project in file(".")).
  settings(commonSettings: _*).
  settings(
    name := "facebook4s",
    publishArtifact in Test := false,
    parallelExecution in Test := false,
    fork in run := false,
    libraryDependencies ++= f4sDeps).
  settings(Format.settings) 

initialCommands in console :=
  s"""
    | import scala.concurrent.duration._
    | import scala.concurrent.Await
    | import scala.concurrent.ExecutionContext.Implicits._
    |
    | import facebook4s._
    | import facebook4s.request._
    | import facebook4s.response._
    | import facebook4s.connection._
    | import facebook4s.api._
    | import facebook4s.api.FacebookMarketingApi._
    | import facebook4s.api.FacebookGraphApi._
    | import com.typesafe.config.ConfigFactory
    |
    | val config = ConfigFactory.load()
    | val accessTokenStr = config.getString("facebook4s.console.access-token")
    |
    | implicit val a = AccessToken(accessTokenStr, 0L)
    | implicit val sa = Some(a)
    | implicit val cfg = new FacebookConnectionInformation
    | implicit val conn = new FacebookConnection
    |
    | val requestBuilder = FacebookRequestBuilder()
    |""".stripMargin

cleanupCommands in console :=
  s"""
     | conn.shutdown()
   """.stripMargin

