package com.bimschas.pwascoring.app

import akka.actor.Scheduler
import akka.stream.ActorMaterializer
import akka.{actor => untyped}
import com.bimschas.pwascoring.ActorBasedContestService
import com.bimschas.pwascoring.rest.RestService
import com.bimschas.pwascoring.rest.RestServiceConfig

import scala.concurrent.Await
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt

object PwaScoringServer extends App {

  private implicit val actorSystem: untyped.ActorSystem = untyped.ActorSystem("pwa-scoring")
  private implicit val materializer: ActorMaterializer = ActorMaterializer()
  private implicit val executionContext: ExecutionContext = actorSystem.dispatcher
  private implicit val scheduler: Scheduler = actorSystem.scheduler

  val contestService = ActorBasedContestService(actorSystem)
  val restService = RestService(RestServiceConfig("localhost", 8080), contestService)

  actorSystem.registerOnTermination(Await.result(restService.shutdown(), 1.minute))
  ShutdownHooks.register(Await.result(actorSystem.terminate(), 1.minute))

  restService.startup()

}
