package com.bimschas.pwascoring.service

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.persistence.typed.scaladsl.PersistentBehaviors
import akka.persistence.typed.scaladsl.PersistentBehaviors.CommandHandler
import akka.persistence.typed.scaladsl.PersistentBehaviors.Effect
import com.bimschas.pwascoring.domain.Heat
import com.bimschas.pwascoring.domain.Heat.EndHeatError
import com.bimschas.pwascoring.domain.Heat.HeatNotPlanned
import com.bimschas.pwascoring.domain.Heat.PlanHeatError
import com.bimschas.pwascoring.domain.Heat.ScoreJumpError
import com.bimschas.pwascoring.domain.Heat.ScoreWaveError
import com.bimschas.pwascoring.domain.Heat.StartHeatError
import com.bimschas.pwascoring.domain.HeatContestants
import com.bimschas.pwascoring.domain.HeatEndedEvent
import com.bimschas.pwascoring.domain.HeatEvent
import com.bimschas.pwascoring.domain.HeatId
import com.bimschas.pwascoring.domain.HeatPlannedEvent
import com.bimschas.pwascoring.domain.HeatRules
import com.bimschas.pwascoring.domain.HeatStartedEvent
import com.bimschas.pwascoring.domain.JumpScore
import com.bimschas.pwascoring.domain.JumpScoredEvent
import com.bimschas.pwascoring.domain.RiderId
import com.bimschas.pwascoring.domain.ScoreSheets
import com.bimschas.pwascoring.domain.WaveScore
import com.bimschas.pwascoring.domain.WaveScoredEvent

object HeatActor {

  ////////////////////////////
  // Commands and Responses //
  ////////////////////////////

  sealed trait HeatCommand

  type PlanHeatResponse = Either[PlanHeatError, HeatPlannedEvent]
  case class PlanHeat(contestants: HeatContestants, rules: HeatRules, replyTo: ActorRef[PlanHeatResponse]) extends HeatCommand

  type StartHeatResponse = Either[StartHeatError, HeatStartedEvent]
  case class StartHeat(replyTo: ActorRef[StartHeatResponse]) extends HeatCommand

  type ScoreWaveResponse = Either[ScoreWaveError, WaveScoredEvent]
  case class ScoreWave(riderId: RiderId, waveScore: WaveScore, replyTo: ActorRef[ScoreWaveResponse]) extends HeatCommand

  type ScoreJumpResponse = Either[ScoreJumpError, JumpScoredEvent]
  case class ScoreJump(riderId: RiderId, jumpScore: JumpScore, replyTo: ActorRef[ScoreJumpResponse]) extends HeatCommand

  type GetScoreSheetsResponse = Either[HeatNotPlanned.type, ScoreSheets]
  case class GetScoreSheets(replyTo: ActorRef[GetScoreSheetsResponse]) extends HeatCommand

  type GetContestantsResponse = Either[HeatNotPlanned.type, HeatContestants]
  case class GetContestants(replyTo: ActorRef[GetContestantsResponse]) extends HeatCommand

  type EndHeatResponse = Either[EndHeatError, HeatEndedEvent]
  case class EndHeat(replyTo: ActorRef[EndHeatResponse]) extends HeatCommand

  case object PassivateHeat extends HeatCommand

  //////////////
  // Behavior //
  //////////////

  def heatBehavior(entityId: String): Behavior[HeatCommand] =
    PersistentBehaviors.immutable[HeatCommand, HeatEvent, Heat](
      persistenceId = entityId,
      initialState = Heat(heatId = HeatId.parse(entityId).get),
      commandHandler = commandHandler,
      eventHandler = heatEventHandler
    )

  ///////////////////
  // Event Handler //
  ///////////////////

  private lazy val heatEventHandler: (Heat, HeatEvent) => Heat =
    (state, event) => state.handleEvent(event)

  /////////////////////
  // Command Handler //
  /////////////////////

  private lazy val commandHandler: CommandHandler[HeatCommand, HeatEvent, Heat] =
    (_, state, command) => command match {

      case PlanHeat(contestants, rules, replyTo) =>
        state.planHeat(contestants, rules) match {
          case Left(error) => Effect.none.andThen(_ => replyTo ! Left(error))
          case Right(heatPlannedEvent) =>
            Effect.persist(heatPlannedEvent).andThen { _ =>
              replyTo ! Right(heatPlannedEvent)
            }
        }

      case GetContestants(replyTo) =>
        Effect.none.andThen(_ =>
          state.scoreSheets match {
            case None => replyTo ! Left(HeatNotPlanned)
            case Some(sheets) => replyTo ! Right(HeatContestants(sheets.scoreSheetsByRider.keySet))
          }
        )

      case GetScoreSheets(replyTo) =>
        Effect.none.andThen(_ =>
          state.scoreSheets match {
            case None => replyTo ! Left(HeatNotPlanned)
            case Some(sheets) => replyTo ! Right(sheets)
          }
        )

      case StartHeat(replyTo) =>
        state.startHeat() match {
          case Left(error) => Effect.none.andThen(_ => replyTo ! Left(error))
          case Right(heatStartedEvent) =>
            Effect.persist(heatStartedEvent).andThen { _ =>
              replyTo ! Right(heatStartedEvent)
            }
        }

      case ScoreJump(riderId, jumpScore, replyTo) =>
        state.scoreJump(riderId, jumpScore) match {
          case Left(error) => Effect.none.andThen(_ => replyTo ! Left(error))
          case Right(jumpScoredEvent) =>
            Effect.persist(jumpScoredEvent).andThen { _ =>
              replyTo ! Right(jumpScoredEvent)
            }
        }

      case ScoreWave(riderId, waveScore, replyTo) =>
        state.scoreWave(riderId, waveScore) match {
          case Left(error) => Effect.none.andThen(_ => replyTo ! Left(error))
          case Right(waveScoredEvent) =>
            Effect.persist(waveScoredEvent).andThen { _ =>
              replyTo ! Right(waveScoredEvent)
            }
        }

      case EndHeat(replyTo) =>
        state.endHeat() match {
          case Left(error) => Effect.none.andThen(_ => replyTo ! Left(error))
          case Right(heatEndedEvent) =>
            Effect.persist(heatEndedEvent).andThen { _ =>
              replyTo ! Right(heatEndedEvent)
            }
        }

      case PassivateHeat =>
        Effect.stop
    }
}
