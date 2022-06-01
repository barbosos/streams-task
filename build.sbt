name := "dumb"

version := "0.1"

scalaVersion := "2.12.7"

libraryDependencies += "com.typesafe.akka" %% "akka-stream" % "2.6.19"
libraryDependencies += "com.typesafe" % "config" % "1.3.3"
libraryDependencies += "com.typesafe.akka" %% "akka-stream-kafka" % "1.0-M1"
libraryDependencies += "io.circe" %% "circe-core" % "0.14.1"
libraryDependencies += "io.circe" %% "circe-generic" % "0.14.1"
libraryDependencies += "io.circe" %% "circe-parser" % "0.14.1"
libraryDependencies += "org.typelevel" %% "cats-effect" % "3.3.12"
libraryDependencies += "com.typesafe.akka" %% "akka-persistence-query" % "2.6.19"
