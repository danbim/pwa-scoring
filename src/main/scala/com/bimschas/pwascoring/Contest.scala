package com.bimschas.pwascoring

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.Props
import akka.actor.typed.scaladsl.Behaviors

object Contest {

  final case class RiderId(lastName: String, firstName: String, sailNr: String)
  final case class HeatId(number: Int, classifier: Option[Char])

  private implicit class HeatIdOps(val heatId: HeatId) extends AnyVal {
    def actorName: String = s"HeatActor_${heatId.number}_${heatId.classifier}"
  }

  sealed trait ContestCommand
  final case class StartHeat(heatId: HeatId, replyTo: ActorRef[Either[HeatAlreadyStarted, HeatStarted]]) extends ContestCommand
  final case class GetHeat(heatId: HeatId, replayTo: ActorRef[Either[HeatIdUnknown, HeatRef]]) extends ContestCommand

  final case class HeatStarted(handle: ActorRef[HeatCommand])
  final case class HeatRef(handle: ActorRef[HeatCommand])
  final case class HeatAlreadyStarted(handle: ActorRef[HeatCommand])
  final case class HeatIdUnknown(heatId: HeatId)

  val initialBehavior: Behavior[ContestCommand] =
    contestBehavior(Map.empty)

  private def contestBehavior(heats: Map[HeatId, ActorRef[HeatCommand]]): Behavior[ContestCommand] =
    Behaviors.immutable[ContestCommand] { (ctx, msg) =>
      msg match {
        case StartHeat(heatId, sender) =>
          heats.get(heatId) match {
            case Some(heat) =>
              sender ! Left(HeatAlreadyStarted(heat))
              contestBehavior(heats)
            case None =>
              val heat = ctx.spawn(heatBehavior(List.empty), heatId.actorName, Props.empty)
              sender ! Right(HeatStarted(heat))
              contestBehavior(heats + (heatId -> heat))
          }
        case GetHeat(heatId, sender) =>
          heats.get(heatId) match {
            case Some(heat) =>
              sender ! Right(HeatRef(heat))
              contestBehavior(heats)
            case None =>
              sender ! Left(HeatIdUnknown(heatId))
              contestBehavior(heats)
          }
      }
    }

  sealed trait HeatCommand
  final case class ScoreWave() extends HeatCommand

  private def heatBehavior(events: List[AnyVal]): Behavior[HeatCommand] =
    Behaviors.immutable[HeatCommand] { (_, msg) =>
      msg match {
        case ScoreWave() =>
          Behaviors.same
      }
    }
}
