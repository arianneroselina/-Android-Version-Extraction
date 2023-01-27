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
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.4",
  "commons-cli" % "commons-cli" % "1.5.0")

ThisBuild / assemblyMergeStrategy := {
  case x if Assembly.isConfigFile(x) => MergeStrategy.concat
  case PathList(ps @ _*) if Assembly.isReadme(ps.last) || Assembly.isLicenseFile(ps.last) =>
    MergeStrategy.rename
  case PathList("META-INF", xs @ _*) =>
    (xs map {_.toLowerCase}) match {
      case ("manifest.mf" :: Nil) | ("index.list" :: Nil) | ("dependencies" :: Nil) =>
        MergeStrategy.discard
      case ps @ (x :: xs) if ps.last.endsWith(".sf") || ps.last.endsWith(".dsa") =>
        MergeStrategy.discard
      case "plexus" :: xs =>
        MergeStrategy.discard
      case "services" :: xs =>
        MergeStrategy.filterDistinctLines
      case ("spring.schemas" :: Nil) | ("spring.handlers" :: Nil) =>
        MergeStrategy.filterDistinctLines
      case _ => MergeStrategy.last
    }
  case _ => MergeStrategy.last
}
