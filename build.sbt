import play.Project._

name := "zentask"

version := "1.0"

libraryDependencies ++= Seq(
  "com.geteit" %% "reactive-couch" % "0.2-SNAPSHOT"
)

resolvers += "spray repo" at "http://repo.spray.io"

resolvers += "reactive-couch releases" at "https://raw.github.com/zbsz/mvn-repo/master/releases/"

resolvers += "reactive-couch snapshots" at "https://raw.github.com/zbsz/mvn-repo/master/snapshots/"

playScalaSettings
