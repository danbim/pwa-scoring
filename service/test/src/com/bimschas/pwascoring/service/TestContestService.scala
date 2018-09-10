package com.bimschas.pwascoring.service

object TestContestService {

  def apply(): ContestService with AutoCloseable = {
    val system = TestActorSystem()
    new ActorBasedContestService(system, ContestActor(system)) with AutoCloseable {
      override def close(): Unit = {
        system.terminate()
        ()
      }
    }
  }
}
