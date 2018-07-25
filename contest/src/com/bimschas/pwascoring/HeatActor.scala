package com.bimschas.pwascoring

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.persistence.typed.scaladsl.PersistentBehaviors
import akka.persistence.typed.scaladsl.PersistentBehaviors.CommandHandler
import akka.persistence.typed.scaladsl.PersistentBehaviors.Effect
import com.bimschas.pwascoring.domain.Heat
import com.bimschas.pwascoring.domain.Heat.RiderIdUnknown
import com.bimschas.pwascoring.domain.HeatContestants
import com.bimschas.pwascoring.domain.HeatEvent
import com.bimschas.pwascoring.domain.HeatId
import com.bimschas.pwascoring.domain.JumpScore
import com.bimschas.pwascoring.domain.RiderId
import com.bimschas.pwascoring.domain.ScoreSheets
import com.bimschas.pwascoring.domain.WaveScore

object HeatActor {

  sealed trait HeatCommand
  final case class ScoreWave(riderId: RiderId, waveScore: WaveScore, replyTo: ActorRef[Either[RiderIdUnknown, WaveScored]]) extends HeatCommand
  final case class ScoreJump(riderId: RiderId, jumpScore: JumpScore, replyTo: ActorRef[Either[RiderIdUnknown, JumpScored]]) extends HeatCommand
  final case class GetScoreSheets(replyTo: ActorRef[ScoreSheets]) extends HeatCommand
  final case class GetContestants(replyTo: ActorRef[HeatContestants]) extends HeatCommand
  final case object PassivateHeat extends HeatCommand

  sealed trait HeatResponse
  final case class WaveScored(riderId: RiderId, waveScore: WaveScore) extends HeatResponse
  final case class JumpScored(riderId: RiderId, jumpScore: JumpScore) extends HeatResponse

  def heatBehavior(entityId: String, heatId: HeatId, contestants: HeatContestants): Behavior[HeatCommand] =
    PersistentBehaviors.immutable[HeatCommand, HeatEvent, Heat](
      persistenceId = entityId,
      initialState = Heat.empty(contestants),
      commandHandler = heatCommandHandler,
      eventHandler = Heat.handleEvent
    )

  private lazy val heatCommandHandler: CommandHandler[HeatCommand, HeatEvent, Heat] = {
    case (_, state, cmd) =>
      cmd match {

        case ScoreWave(riderId, waveScore, replyTo) =>
          state.scoreWave(riderId, waveScore) match {
            case Left(unknownRiderId) =>
              Effect.none.andThen(_ => replyTo ! Left(unknownRiderId))
            case Right(waveScoredEvent) =>
              Effect.persist(waveScoredEvent).andThen(_ => replyTo ! Right(WaveScored(riderId, waveScore)))
          }

        case ScoreJump(riderId, jumpScore, replyTo) =>
          state.scoreJump(riderId, jumpScore) match {
            case Left(unknownRiderId) =>
              Effect.none.andThen(_ => replyTo ! Left(unknownRiderId))
            case Right(jumpScoredEvent) =>
              Effect.persist(jumpScoredEvent).andThen(_ => replyTo ! Right(JumpScored(riderId, jumpScore)))
          }

        case GetScoreSheets(replyTo) =>
          replyTo ! state.scoreSheets
          Effect.none

        case GetContestants(replyTo) =>
          replyTo ! state.contestants
          Effect.none

        case PassivateHeat =>
          Effect.stop
      }
  }
}
