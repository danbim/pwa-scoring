package com.bimschas.pwascoring

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.persistence.typed.scaladsl.PersistentBehaviors
import akka.persistence.typed.scaladsl.PersistentBehaviors.CommandHandler
import akka.persistence.typed.scaladsl.PersistentBehaviors.Effect
import com.bimschas.pwascoring.model.HeatContestants
import com.bimschas.pwascoring.model.HeatId
import com.bimschas.pwascoring.model.HeatState
import com.bimschas.pwascoring.model.JumpScore
import com.bimschas.pwascoring.model.RiderId
import com.bimschas.pwascoring.model.RiderScoreSheet
import com.bimschas.pwascoring.model.WaveScore

object Heat {

  ////// commands
  sealed trait HeatCommand
  final case class ScoreWave(waveScore: WaveScore, replyTo: ActorRef[WaveScored]) extends HeatCommand
  final case class ScoreJump(jumpScore: JumpScore, replyTo: ActorRef[JumpScored]) extends HeatCommand
  final case class GetScoreSheets(replyTo: ActorRef[Map[RiderId, RiderScoreSheet]]) extends HeatCommand
  final case object PassivateHeat extends HeatCommand

  ////// command responses
  final case class WaveScored(waveScore: WaveScore)
  final case class JumpScored(waveScore: JumpScore)

  ////// events
  sealed trait HeatEvent
  final case class WaveScoredEvent(waveScore: WaveScore) extends HeatEvent
  final case class JumpScoredEvent(jumpScore: JumpScore) extends HeatEvent

  ////// event handler
  private val heatEventHandler: (HeatState, HeatEvent) => HeatState = {
    case (state, event) =>
      event match {
        case WaveScoredEvent(waveScore) => state + waveScore
        case JumpScoredEvent(jumpScore) => state + jumpScore
      }
  }

  ////// behavior / command handler
  def heatBehavior(entityId: String, heatId: HeatId, contestants: HeatContestants): Behavior[HeatCommand] =
    PersistentBehaviors.immutable[HeatCommand, HeatEvent, HeatState](
      persistenceId = entityId,
      initialState = HeatState.empty(contestants),
      commandHandler = heatCommandHandler,
      eventHandler = heatEventHandler
    )

  private val heatCommandHandler: CommandHandler[HeatCommand, HeatEvent, HeatState] = {
    case (_, state, cmd) =>
      cmd match {
        case ScoreWave(waveScore, replyTo) =>
          Effect.persist(WaveScoredEvent(waveScore)).andThen { _ =>
            replyTo ! WaveScored(waveScore)
          }
        case ScoreJump(jumpScore, replyTo) =>
          Effect.persist(JumpScoredEvent(jumpScore)).andThen { _ =>
            replyTo ! JumpScored(jumpScore)
          }
        case GetScoreSheets(replyTo) =>
          replyTo ! state.scoreSheets
          Effect.none
        case PassivateHeat =>
          Effect.stop
      }
  }
}
