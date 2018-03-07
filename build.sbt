name := "pwa-scoring"

version := "1.0"

scalaVersion := "2.12.2"

lazy val akkaVersion = "2.5.11"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,
  "com.typesafe.akka" %% "akka-persistence-typed" % akkaVersion,
  "com.typesafe.akka" %% "akka-cluster-sharding-typed" % akkaVersion,
  "com.typesafe.akka" %% "akka-testkit-typed" % akkaVersion % Test,
  "org.scalatest" %% "scalatest" % "3.0.1" % "test",
  "org.fusesource.leveldbjni" % "leveldbjni-all" % "1.8"
)
