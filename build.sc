// build.sc
import mill._
import mill.scalalib._

object Settings {
  val scalaVersion = "2.12.6"
  val akkaVersion = "2.5.11"
}

object domain extends ScalaModule {
  override def scalaVersion = Settings.scalaVersion
  object test extends Tests {
    override def testFrameworks = Seq("org.scalatest.tools.Framework")
    override def ivyDeps = Agg(
      ivy"org.scalatest::scalatest:3.0.1",
			ivy"org.scalacheck::scalacheck:1.14.0"
    )
  }
}

object app extends SbtModule {
  override def moduleDeps = Seq(domain)
  override def scalaVersion = Settings.scalaVersion
}

object contest extends SbtModule {
  override def moduleDeps = Seq(domain)
  override def scalaVersion = Settings.scalaVersion
  override def ivyDeps = Agg(
    ivy"com.typesafe.akka::akka-actor-typed:${Settings.akkaVersion}",
    ivy"com.typesafe.akka::akka-persistence-typed:${Settings.akkaVersion}",
    ivy"com.typesafe.akka::akka-cluster-sharding-typed:${Settings.akkaVersion}"
  )
  object test extends Tests {
    override def testFrameworks = Seq("org.scalatest.tools.Framework")
    override def ivyDeps = Agg(
      ivy"com.typesafe.akka::akka-testkit-typed:${Settings.akkaVersion}",
      ivy"org.scalatest::scalatest:3.0.1",
			ivy"org.scalacheck::scalacheck:1.14.0",
      ivy"org.fusesource.leveldbjni:leveldbjni-all:1.8"
    )
  }
}
