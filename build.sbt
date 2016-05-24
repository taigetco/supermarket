name := "supermarket"

version := "1.0"

scalaVersion := "2.11.8"

resolvers ++= Seq(
  "Typesafe repo" at "http://repo.typesafe.com/typesafe/releases/")

resolvers += Resolver.mavenLocal

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.4.6",
  "com.typesafe.akka" %% "akka-slf4j" % "2.4.6",
  "org.slf4j" % "slf4j-api" % "1.7.7",
  "org.slf4j" % "jcl-over-slf4j" % "1.7.7",
  "org.slf4j" % "log4j-over-slf4j" % "1.7.7",
  "ch.qos.logback" % "logback-classic" % "1.1.2",
  "com.typesafe.akka" %% "akka-testkit" % "2.4.6" % "test"
)


