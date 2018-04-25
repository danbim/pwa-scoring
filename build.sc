// build.sc
import mill._
import mill.scalalib._

object Settings {
	val scalaVersion = "2.12.4"
	val akkaVersion = "2.5.11"
}

object domain extends SbtModule {
  def scalaVersion = Settings.scalaVersion
	object test extends Tests {
		def testFrameworks = Seq("org.scalatest.tools.Framework")
		def ivyDeps = Agg(ivy"org.scalatest::scalatest:3.0.1")
	}
}

object app extends SbtModule {
	def moduleDeps = Seq(domain)
	def scalaVersion = Settings.scalaVersion
}

object contest extends SbtModule {
	def moduleDeps = Seq(domain)
	def scalaVersion = Settings.scalaVersion
	def ivyDeps = Agg(
		ivy"com.typesafe.akka::akka-actor-typed:${Settings.akkaVersion}",
  	ivy"com.typesafe.akka::akka-persistence-typed:${Settings.akkaVersion}",
    ivy"com.typesafe.akka::akka-cluster-sharding-typed:${Settings.akkaVersion}"
	)
	object test extends Tests {
		def testFrameworks = Seq("org.scalatest.tools.Framework")
		def ivyDeps = Agg(
      ivy"com.typesafe.akka::akka-testkit-typed:${Settings.akkaVersion}",
      ivy"org.scalatest::scalatest:3.0.1",
      ivy"org.fusesource.leveldbjni:leveldbjni-all:1.8"
		)
	}
}
