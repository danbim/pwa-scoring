package com.bimschas.pwascoring.service

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.adapter._
import akka.{actor => untyped}

object TestActorSystem {

  def apply(): ActorSystem[_] = {

    val actorSystemName = TestActorSystemName.next()
    untyped
      .ActorSystem(actorSystemName, TestActorSystemConfig(actorSystemName))
      .toTyped
  }
}

object TestActorSystemName {

  def next(): String =
    s"pwa-scoring-test-${IdGenerator.nextId()}"
}
