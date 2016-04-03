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
     | import akka.actor.ActorSystem
     |
     | import facebook4s.api.{ AccessToken, FacebookMarketingApi }
     | import facebook4s.connection.FacebookConnectionInformation
     | import facebook4s.request.{ FacebookBatchRequestBuilder, FacebookGetRequest }
     | import facebook4s.response.FacebookTimePaging
     | import facebook4s.api.FacebookMarketingApi._
     | import facebook4s.api.FacebookGraphApi._
     |
     | import http.client.connection.impl.{ PlayWSHttpConnection, ThrottledHttpConnection }
     |
     | import play.api.libs.json.{ JsArray, Json }
     |
     | import scala.concurrent.Future
     | import scala.concurrent.duration._
     | import scala.concurrent.Await
     | import scala.concurrent.ExecutionContext.Implicits.global
     |
     | import com.typesafe.config.ConfigFactory
     |
     | val config = ConfigFactory.load()
     | val accessTokenStr = config.getString("facebook4s.console.access-token")
     | val accessTokenOpt = Some(AccessToken(accessTokenStr, 0L))
     |
     | lazy val cfg: FacebookConnectionInformation = FacebookConnectionInformation()
     |
     | val connection = new ThrottledHttpConnection {
     |   override val actorSystem = ActorSystem("facebook4s-console")
     |   override val connection = new PlayWSHttpConnection
     | }
     |
     | val requestBuilder = new FacebookBatchRequestBuilder(cfg, connection, accessTokenOpt)
     |""".stripMargin

cleanupCommands in console :=
  s"""
     | requestBuilder.shutdown()
   """.stripMargin

