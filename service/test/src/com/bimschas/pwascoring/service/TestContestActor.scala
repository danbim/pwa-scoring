package com.bimschas.pwascoring.service

import akka.actor.typed.ActorRef
import akka.actor.typed.ActorSystem

object TestContestActor {

  def apply(): ActorRef[ContestActor.ContestCommand] =
    apply(TestActorSystem())

  def apply(system: ActorSystem[_]): ActorRef[ContestActor.ContestCommand] =
    ContestActor(system)
}
