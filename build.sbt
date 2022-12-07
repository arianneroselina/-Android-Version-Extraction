name := "android_version_extraction"
organization := "com.extraction"
version := "0.1.0-SNAPSHOT"

(ThisBuild / scalaVersion) := "2.13.9"

(ThisBuild / developers) := List(
  Developer(
    id = "arp",
    name = "Arianne Roselina Prananto",
    email = "vincentiarianne@gmail.com",
    url = url("https://github.com/arianneroselina")))

(ThisBuild / libraryDependencies) := List (
  "com.typesafe.play" %% "play-json" % "2.9.3",
  "org.scala-lang" % "scala-reflect" % "2.13.8",
  "ch.qos.logback" % "logback-classic" % "1.2.10",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.4")
