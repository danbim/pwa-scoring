// build.sc
import mill._
import mill.define.Target
import mill.scalalib._

object Dependencies {

  object Versions {

    val scalaVersion = "2.12.6"
    val akkaVersion = "2.5.11"
    val akkaHttpVersion = "10.1.3"
    val akkaHttpCorsVersion = "0.3.1"
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
  val akkaHttpCors = ivy"ch.megard::akka-http-cors:$akkaHttpCorsVersion"

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
  override def scalacOptions: Target[Seq[String]] = Seq(
    "-deprecation",                      // Emit warning and location for usages of deprecated APIs.
    "-encoding", "utf-8",                // Specify character encoding used by source files.
    "-explaintypes",                     // Explain type errors in more detail.
    "-feature",                          // Emit warning and location for usages of features that should be imported explicitly.
    "-language:higherKinds",             // Allow higher-kinded types
    "-language:implicitConversions",     // Allow definition of implicit functions called views
    "-unchecked",                        // Enable additional warnings where generated code depends on assumptions.
    "-Xcheckinit",                       // Wrap field accessors to throw an exception on uninitialized access.
    "-Xfatal-warnings",                  // Fail the compilation if there are any warnings.
    "-Xlint:constant",                   // Evaluation of a constant arithmetic expression results in an error.
    "-Xlint:delayedinit-select",         // Selecting member of DelayedInit.
    "-Xlint:doc-detached",               // A Scaladoc comment appears to be detached from its element.
    "-Xlint:inaccessible",               // Warn about inaccessible types in method signatures.
    "-Xlint:infer-any",                  // Warn when a type argument is inferred to be `Any`.
    "-Xlint:missing-interpolator",       // A string literal appears to be missing an interpolator id.
    "-Xlint:nullary-override",           // Warn when non-nullary `def f()' overrides nullary `def f'.
    "-Xlint:nullary-unit",               // Warn when nullary methods return Unit.
    "-Xlint:option-implicit",            // Option.apply used implicit view.
    "-Xlint:package-object-classes",     // Class or object defined in package object.
    "-Xlint:poly-implicit-overload",     // Parameterized overloaded implicit methods are not visible as view bounds.
    "-Xlint:private-shadow",             // A private field (or class parameter) shadows a superclass field.
    "-Xlint:stars-align",                // Pattern sequence wildcard must align with sequence component.
    "-Xlint:type-parameter-shadow",      // A local type parameter shadows a type already in scope.
    "-Xlint:unsound-match",              // Pattern match may not be typesafe.
    "-Yno-adapted-args",                 // Do not adapt an argument list (either by inserting () or creating a tuple) to match the receiver.
    "-Ywarn-dead-code",                  // Warn when dead code is identified.
    "-Ywarn-extra-implicit",             // Warn when more than one implicit parameter section is defined.
    "-Ywarn-inaccessible",               // Warn about inaccessible types in method signatures.
    "-Ywarn-infer-any",                  // Warn when a type argument is inferred to be `Any`.
    "-Ywarn-nullary-override",           // Warn when non-nullary `def f()' overrides nullary `def f'.
    "-Ywarn-nullary-unit",               // Warn when nullary methods return Unit.
    "-Ywarn-numeric-widen",              // Warn when numerics are widened.
    "-Ywarn-unused:implicits",           // Warn if an implicit parameter is unused.
    "-Ywarn-unused:imports",             // Warn if an import selector is not referenced.
    "-Ywarn-unused:locals",              // Warn if a local definition is unused.
    "-Ywarn-unused:params",              // Warn if a value parameter is unused.
    "-Ywarn-unused:patvars",             // Warn if a variable bound in a pattern is unused.
    "-Ywarn-unused:privates",            // Warn if a private member is unused.
    "-Ywarn-value-discard"               // Warn when non-Unit expression results are unused.
  )
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
    override def moduleDeps: Seq[JavaModule] = super.moduleDeps ++ Seq(domain.test)
  }
}

object rest extends ScalaModuleBase {
  import Dependencies._

  override def moduleDeps = Seq(domain, service)
  override def ivyDeps = Agg(akkaHttp, akkaHttpSprayJson, akkaPersistenceQuery, akkaHttpCors)
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
