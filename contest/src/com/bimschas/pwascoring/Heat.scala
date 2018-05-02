package com.bimschas.pwascoring

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior

import akka.cluster.sharding.typed.ClusterShardingSettings
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.cluster.sharding.typed.scaladsl.EntityRef

import akka.persistence.typed.scaladsl.PersistentBehaviors
import akka.persistence.typed.scaladsl.PersistentBehaviors.CommandHandler
import akka.persistence.typed.scaladsl.PersistentBehaviors.Effect

import com.bimschas.pwascoring.domain.HeatContestants
import com.bimschas.pwascoring.domain.HeatId
import com.bimschas.pwascoring.domain.HeatState
import com.bimschas.pwascoring.domain.JumpScore
import com.bimschas.pwascoring.domain.RiderId
import com.bimschas.pwascoring.domain.ScoreSheet
import com.bimschas.pwascoring.domain.WaveScore

object Heat {

  ////// commands
  sealed trait HeatCommand
  final case class ScoreWave(riderId: RiderId, waveScore: WaveScore, replyTo: ActorRef[Either[UnknownRiderId, WaveScored]]) extends HeatCommand
  final case class ScoreJump(riderId: RiderId, jumpScore: JumpScore, replyTo: ActorRef[Either[UnknownRiderId, JumpScored]]) extends HeatCommand
  final case class GetScoreSheets(replyTo: ActorRef[Map[RiderId, ScoreSheet]]) extends HeatCommand
  final case object PassivateHeat extends HeatCommand

  ////// command responses
  final case class WaveScored(riderId: RiderId, waveScore: WaveScore)
  final case class JumpScored(riderId: RiderId, waveScore: JumpScore)

  ////// events
  sealed trait HeatEvent
  final case class WaveScoredEvent(riderId: RiderId, waveScore: WaveScore) extends HeatEvent
  final case class JumpScoredEvent(riderId: RiderId, jumpScore: JumpScore) extends HeatEvent

  ////// errors
  sealed trait Errors
  final case class UnknownRiderId(riderId: RiderId)

  ////// event handler
  private val heatEventHandler: (HeatState, HeatEvent) => HeatState = {
    case (state, event) =>
      event match {
        case WaveScoredEvent(riderId, waveScore) => state + (riderId, waveScore)
        case JumpScoredEvent(riderId, jumpScore) => state + (riderId, jumpScore)
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
        case ScoreWave(riderId, waveScore, replyTo) =>
          if (state.contestants.riderIds.contains(riderId)) {
            Effect.persist(WaveScoredEvent(riderId, waveScore)).andThen { _ =>
              replyTo ! Right(WaveScored(riderId, waveScore))
            }
          } else {
            replyTo ! Left(UnknownRiderId(riderId))
            Effect.none
          }
        case ScoreJump(riderId, jumpScore, replyTo) =>
          if (state.contestants.riderIds.contains(riderId)) {
            Effect.persist(JumpScoredEvent(riderId, jumpScore)).andThen { _ =>
              replyTo ! Right(JumpScored(riderId, jumpScore))
            }
          } else {
            replyTo ! Left(UnknownRiderId(riderId))
            Effect.none
          }
        case GetScoreSheets(replyTo) =>
          replyTo ! state.scoreSheets
          Effect.none
        case PassivateHeat =>
          Effect.stop
      }
  }
}
