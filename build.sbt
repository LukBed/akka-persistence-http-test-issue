name := "persistence-http-test"

version := "0.1"

scalaVersion := "2.13.6"

val AkkaVersion = "2.6.14"
val AkkaHttpVersion = "10.2.4"
val ScalatestVersion = "3.2.5"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor-typed" % AkkaVersion,
  "com.typesafe.akka" %% "akka-stream-typed" % AkkaVersion,
  "com.typesafe.akka" %% "akka-http" % AkkaHttpVersion,
  "com.typesafe.akka" %% "akka-persistence-typed" % AkkaVersion,
  "org.scalatest" %% "scalatest" % ScalatestVersion,
  "com.typesafe.akka" %% "akka-stream-testkit" % AkkaVersion,
  "com.typesafe.akka" %% "akka-http-testkit" % AkkaHttpVersion,
  "com.typesafe.akka" %% "akka-actor-testkit-typed" % AkkaVersion,
  "com.typesafe.akka" %% "akka-persistence-testkit" % AkkaVersion
)


