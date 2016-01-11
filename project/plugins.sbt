// Comment to get more information during initialization
logLevel := Level.Warn

resolvers ++= Seq(
  "Sonatype snapshots" at "http://oss.sonatype.org/content/repositories/snapshots/",
	"Typesafe Releases" at "http://repo.typesafe.com/typesafe/releases/",
	Classpaths.sbtPluginReleases
)

addSbtPlugin("com.timushev.sbt" % "sbt-updates" % "0.1.5")

addSbtPlugin("io.spray" % "sbt-revolver" % "0.7.2")

addSbtPlugin("org.scoverage" % "sbt-scoverage" % "0.99.5")

addSbtPlugin("com.typesafe.sbt" % "sbt-scalariform" % "1.3.0")

