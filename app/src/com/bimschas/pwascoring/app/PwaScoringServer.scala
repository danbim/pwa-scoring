package com.bimschas.pwascoring.app

import akka.stream.ActorMaterializer
import akka.{actor => untyped}
import com.bimschas.pwascoring.rest.RestService
import com.bimschas.pwascoring.rest.RestServiceConfig
import com.bimschas.pwascoring.service.ActorBasedContestService

import scala.concurrent.Await
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt

object PwaScoringServer extends App {

  private implicit val actorSystem: untyped.ActorSystem = untyped.ActorSystem("pwa-scoring")
  private implicit val materializer: ActorMaterializer = ActorMaterializer()
  private implicit val executionContext: ExecutionContext = actorSystem.dispatcher

  val contestService = ActorBasedContestService(actorSystem)
  val restService = RestService(RestServiceConfig("localhost", 8080), contestService)

  actorSystem.registerOnTermination {
    println(s"Shutting down REST service...")
    Await.result(restService.shutdown(), 1.minute)
    println(s"REST service was shut down")
  }
  ShutdownHooks.register {
    println(s"Shutting down ActorSystem...")
    Await.result(actorSystem.terminate(), 1.minute)
    println(s"ActorSystem was shut down")
  }

  restService.startup()

}
