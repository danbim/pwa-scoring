package com.bimschas.pwascoring.app

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.adapter._
import akka.actor.Scheduler
import akka.actor.typed.Props
import akka.cluster.typed.ClusterSingleton
import akka.cluster.typed.ClusterSingletonSettings
import akka.stream.ActorMaterializer
import com.bimschas.pwascoring.ActorBasedContestService
import com.bimschas.pwascoring.ContestActor
import com.bimschas.pwascoring.rest.RestService
import com.bimschas.pwascoring.rest.RestServiceConfig

import scala.concurrent.Await
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt

object PwaScoringServer extends App {

  private implicit val untypedSystem: akka.actor.ActorSystem = akka.actor.ActorSystem("pwa-scoring")
  private implicit val materializer: ActorMaterializer = ActorMaterializer()
  private implicit val executionContext: ExecutionContext = untypedSystem.dispatcher
  private implicit val scheduler: Scheduler = untypedSystem.scheduler

  val system: ActorSystem[_] = untypedSystem.toTyped
  val singletonManager = ClusterSingleton(system)
  val contestActor = singletonManager.spawn(
    behavior = ContestActor.behavior,
    "ContestActor",
    Props.empty,
    ClusterSingletonSettings(system),
    ContestActor.PassivateContest
  )

  val contestService = ActorBasedContestService(contestActor)
  val webService = RestService(RestServiceConfig("localhost", 8080), contestService)
  Runtime.getRuntime.addShutdownHook(new Thread("pwa-scoring-shutdown-thread") {
    override def run(): Unit = Await.result(webService.shutdown(), 1.minute)
  })
  webService.startup()


}
