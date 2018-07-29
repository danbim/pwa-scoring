// build.sc
import mill._
import mill.scalalib._

object Settings {
  val scalaVersion = "2.12.6"
  val akkaVersion = "2.5.11"
  val akkaHttpVersion = "10.1.3"
  val scalaTestVersion = "3.0.5"
  val scalaCheckVersion = "1.14.0"
}

def compile = T {
  domain.compile
  contest.compile
  rest.compile
  app.compile
}

object test extends Module {
  def compile = T {
    domain.test.compile
    contest.test.compile
    rest.test.compile
    app.test.compile
  }
}

trait ScalaModuleBase extends ScalaModule {
  override def scalaVersion: T[String] = Settings.scalaVersion
  trait TestsBase extends Tests {
    override def testFrameworks: T[Seq[String]] =
      Seq("org.scalatest.tools.Framework")
    override def ivyDeps: T[Agg[Dep]] =
      T(Agg(
        ivy"org.scalatest::scalatest:${Settings.scalaTestVersion}",
        ivy"org.scalacheck::scalacheck:${Settings.scalaCheckVersion}"
      ) ++ moreIvyDeps)
    def moreIvyDeps: Agg[Dep] = Agg.empty
  }
}

object domain extends ScalaModuleBase {
  object test extends TestsBase
}

object contest extends ScalaModuleBase {
  import Settings._

  override def moduleDeps = Seq(domain)
  override def ivyDeps = Agg(
    ivy"com.typesafe.akka::akka-actor-typed:$akkaVersion",
    ivy"com.typesafe.akka::akka-persistence-typed:$akkaVersion",
    ivy"com.typesafe.akka::akka-cluster-sharding-typed:$akkaVersion"
  )
  object test extends TestsBase {
    override def moreIvyDeps: Agg[Dep] = Agg(
      ivy"com.typesafe.akka::akka-testkit-typed:$akkaVersion",
      ivy"org.fusesource.leveldbjni:leveldbjni-all:1.8"
    )
  }
}

object rest extends ScalaModuleBase {
  import Settings._

  override def moduleDeps = Seq(domain, contest)
  override def ivyDeps = Agg(
    ivy"com.typesafe.akka::akka-http:$akkaHttpVersion",
    ivy"com.typesafe.akka::akka-http-spray-json:$akkaHttpVersion"
  )
  object test extends TestsBase {
    override def moduleDeps = super.moduleDeps ++ Seq(domain.test)
    override def moreIvyDeps = Agg(
      ivy"com.typesafe.akka::akka-http-testkit:$akkaHttpVersion"
    )
  }
}

object app extends ScalaModuleBase {
  override def moduleDeps = Seq(domain, contest, rest)
  override def mainClass = Some("com.bimschas.pwascoring.app.PwaScoringServer")
  object test extends TestsBase
}
