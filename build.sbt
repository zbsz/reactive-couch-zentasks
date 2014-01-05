import play.Project._

name := "zentask"

version := "1.0"

libraryDependencies ++= Seq(
  jdbc,
  anorm,
  "com.geteit" %% "reactive-couch" % "0.2-SNAPSHOT"
)

playScalaSettings
