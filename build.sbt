name := "play-streak"

version := "1.0.0"

scalaVersion := """2.11.6"""

libraryDependencies ++= Seq(
  "org.webjars" %% "webjars-play" % "2.4.0",
  "org.apache.httpcomponents" % "httpclient" % "4.5"
)

lazy val root = (project in file(".")).enablePlugins(PlayScala)
