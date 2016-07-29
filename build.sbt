import sbt._
import Dependencies._

lazy val commonSettings = Seq(
  name := "social4s",
  version := "1.0.1",
  organization := "social4s",
  scalaVersion := "2.11.7",
  resolvers ++= Seq(Resolver.mavenLocal,
    "Sonatype OSS Releases" at "http://oss.sonatype.org/content/repositories/releases/",
    "Sonatype OSS Snapshots" at "http://oss.sonatype.org/content/repositories/snapshots/",
    "Typesafe Snapshots" at "http://repo.typesafe.com/typesafe/snapshots/",
    "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/",
    "JCenter" at "http://jcenter.bintray.com/"))

lazy val httpClientDeps = Seq(
  bucket4j,
  playJson,
  playWs,
  scalaTest,
  scalaTestPlus,
  typesafeConfig)

lazy val f4sDeps = Seq(
  playJson,
  playWs,
  scalaTest,
  scalaTestPlus,
  typesafeConfig)

lazy val t4sDeps = Seq(
  playJson,
  playWs,
  scalaTest,
  scalaTestPlus,
  typesafeConfig)

lazy val g4sDeps = Seq(
  googleAnalytics,
  googleAnalytics,
  googleOAuth,
  playJson,
  playWs,
  scalaTest,
  scalaTestPlus,
  typesafeConfig)

lazy val root = (project in file(".")).
  settings(commonSettings: _*).
  settings(name := "social4s").
  aggregate(httpClient, f4s, t4s, g4s)

lazy val httpClient = (project in file("http-client")).
  settings(commonSettings: _*).
  settings(
    name := "http-client",
    publishArtifact in Test := false,
    parallelExecution in Test := false,
    fork in run := false,
    libraryDependencies ++= httpClientDeps).
  settings(Format.settings) 

lazy val f4s = (project in file("facebook4s")).
  settings(commonSettings: _*).
  settings(
    name := "facebook4s",
    publishArtifact in Test := false,
    parallelExecution in Test := false,
    fork in run := false,
    libraryDependencies ++= f4sDeps,
    initialCommands in console := scala.io.Source.fromFile("repl/f4s.scala").mkString).
  settings(Format.settings) dependsOn httpClient

lazy val t4s = (project in file("twitter4s")).
  settings(commonSettings: _*).
  settings(
    name := "twitter4s",
    publishArtifact in Test := false,
    parallelExecution in Test := false,
    fork in run := false,
    libraryDependencies ++= t4sDeps,
    initialCommands in console := scala.io.Source.fromFile("repl/t4s.scala").mkString).
  settings(Format.settings) dependsOn httpClient

lazy val g4s = (project in file("google4s")).
  settings(commonSettings: _*).
  settings(
    name := "google4s",
    publishArtifact in Test := false,
    parallelExecution in Test := false,
    fork in run := false,
    libraryDependencies ++= g4sDeps).
  settings(Format.settings) dependsOn httpClient

