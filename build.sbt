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
  scalaTest
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

