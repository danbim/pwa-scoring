// build.sc
import mill._
import mill.scalalib._

object Dependencies {

  object Versions {

    val scalaVersion = "2.12.6"
    val akkaVersion = "2.5.11"
    val akkaHttpVersion = "10.1.3"
    val scalaTestVersion = "3.0.5"
    val scalaCheckVersion = "1.14.0"
    val sprayJsonVersion = "1.3.4"
  }

  import Versions._

  val scalaTestFramework = "org.scalatest.tools.Framework"
  val scalaTest = ivy"org.scalatest::scalatest:$scalaTestVersion"
  val scalaCheck = ivy"org.scalacheck::scalacheck:$scalaCheckVersion"

  val akkaActorTyped = ivy"com.typesafe.akka::akka-actor-typed:$akkaVersion"
  val akkaPersistenceTyped = ivy"com.typesafe.akka::akka-persistence-typed:$akkaVersion"
	val akkaPersistenceQuery = ivy"com.typesafe.akka::akka-persistence-query:$akkaVersion"
  val akkaClusterShardingTyped = ivy"com.typesafe.akka::akka-cluster-sharding-typed:$akkaVersion"
  val akkaTestKitTyped = ivy"com.typesafe.akka::akka-testkit-typed:$akkaVersion"

  val akkaHttp = ivy"com.typesafe.akka::akka-http:$akkaHttpVersion"
  val akkaHttpSprayJson = ivy"com.typesafe.akka::akka-http-spray-json:$akkaHttpVersion"
  val akkaHttpTestKit = ivy"com.typesafe.akka::akka-http-testkit:$akkaHttpVersion"

  val sprayJson = ivy"io.spray::spray-json:$sprayJsonVersion"

  val levelDb = ivy"org.fusesource.leveldbjni:leveldbjni-all:1.8"
}

def compile = T {
  domain.compile
  service.compile
  rest.compile
  app.compile
}

object test extends Module {
  def compile = T {
    domain.test.compile
    service.test.compile
    rest.test.compile
    app.test.compile
  }
}

trait ScalaModuleBase extends ScalaModule {
  import Dependencies._
  override def scalaVersion: T[String] = Dependencies.Versions.scalaVersion
  trait TestsBase extends Tests {
    override def testFrameworks: T[Seq[String]] = Seq(scalaTestFramework)
    override def ivyDeps: T[Agg[Dep]] = T(Agg(scalaTest, scalaCheck) ++ moreIvyDeps)
    def moreIvyDeps: Agg[Dep] = Agg.empty
  }
}

object domain extends ScalaModuleBase {
  import Dependencies._
  override def ivyDeps = Agg(sprayJson)
  object test extends TestsBase
}

object service extends ScalaModuleBase {
  import Dependencies._

  override def moduleDeps = Seq(domain)
  override def ivyDeps = Agg(akkaActorTyped, akkaPersistenceTyped, akkaClusterShardingTyped, levelDb)
  object test extends TestsBase {
    override def moreIvyDeps: Agg[Dep] = Agg(akkaTestKitTyped)
  }
}

object rest extends ScalaModuleBase {
  import Dependencies._

  override def moduleDeps = Seq(domain, service)
  override def ivyDeps = Agg(akkaHttp, akkaHttpSprayJson, akkaPersistenceQuery)
  object test extends TestsBase {
    override def moduleDeps = super.moduleDeps ++ Seq(domain.test)
    override def moreIvyDeps = Agg(akkaHttpTestKit)
  }
}

object app extends ScalaModuleBase {
  override def moduleDeps = Seq(domain, service, rest)
  override def mainClass = Some("com.bimschas.pwascoring.app.PwaScoringServer")
  object test extends TestsBase
}
