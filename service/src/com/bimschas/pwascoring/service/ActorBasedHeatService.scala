package com.bimschas.pwascoring.service

import akka.actor.Scheduler
import akka.cluster.sharding.typed.scaladsl.EntityRef
import akka.util.Timeout
import com.bimschas.pwascoring.domain.Heat.EndHeatError
import com.bimschas.pwascoring.domain.Heat.HeatNotPlanned
import com.bimschas.pwascoring.domain.Heat.PlanHeatError
import com.bimschas.pwascoring.domain.Heat.ScoreJumpError
import com.bimschas.pwascoring.domain.Heat.ScoreWaveError
import com.bimschas.pwascoring.domain.Heat.StartHeatError
import com.bimschas.pwascoring.domain.HeatContestants
import com.bimschas.pwascoring.domain.HeatEndedEvent
import com.bimschas.pwascoring.domain.HeatPlannedEvent
import com.bimschas.pwascoring.domain.HeatRules
import com.bimschas.pwascoring.domain.HeatStartedEvent
import com.bimschas.pwascoring.domain.JumpScore
import com.bimschas.pwascoring.domain.JumpScoredEvent
import com.bimschas.pwascoring.domain.RiderId
import com.bimschas.pwascoring.domain.ScoreSheets
import com.bimschas.pwascoring.domain.WaveScore
import com.bimschas.pwascoring.domain.WaveScoredEvent
import com.bimschas.pwascoring.service.HeatActor.EndHeat
import com.bimschas.pwascoring.service.HeatActor.GetContestants
import com.bimschas.pwascoring.service.HeatActor.GetScoreSheets
import com.bimschas.pwascoring.service.HeatActor.HeatCommand
import com.bimschas.pwascoring.service.HeatActor.PlanHeat
import com.bimschas.pwascoring.service.HeatActor.ScoreJump
import com.bimschas.pwascoring.service.HeatActor.ScoreWave
import com.bimschas.pwascoring.service.HeatActor.StartHeat
import com.bimschas.pwascoring.service.HeatService.HeatServiceError
import scalaz.zio.Callback
import scalaz.zio.ExitResult.Completed
import scalaz.zio.ExitResult.Failed
import scalaz.zio.IO

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import scala.util.Failure
import scala.util.Success

case class ActorBasedHeatService(heatEntity: EntityRef[HeatCommand])(implicit scheduler: Scheduler) extends HeatService {

  private implicit val timeout: Timeout = Timeout(30.seconds)
  private implicit val ec: ExecutionContext = Implicits.global

  override def planHeat(contestants: HeatContestants, rules: HeatRules): IO[Either[HeatServiceError, PlanHeatError], HeatPlannedEvent] =
    io(heatEntity ? (ref => PlanHeat(contestants, rules, ref)))

  override def contestants(): IO[Either[HeatServiceError, HeatNotPlanned.type], HeatContestants] =
    io(heatEntity ? (ref => GetContestants(ref)))

  override def scoreSheets(): IO[Either[HeatServiceError, HeatNotPlanned.type], ScoreSheets] =
    io(heatEntity ? (ref => GetScoreSheets(ref)))

  override def startHeat(): IO[Either[HeatServiceError, StartHeatError], HeatStartedEvent] =
    io(heatEntity ? (ref => StartHeat(ref)))

  override def score(riderId: RiderId, waveScore: WaveScore): IO[Either[HeatServiceError, ScoreWaveError], WaveScoredEvent] =
    io(heatEntity ? (ref => ScoreWave(riderId, waveScore, ref)))

  override def score(riderId: RiderId, jumpScore: JumpScore): IO[Either[HeatServiceError, ScoreJumpError], JumpScoredEvent] =
    io(heatEntity ? (ref => ScoreJump(riderId, jumpScore, ref)))

  override def endHeat(): IO[Either[HeatServiceError, EndHeatError], HeatEndedEvent] =
    io(heatEntity ? (ref => EndHeat(ref)))

  private def io[E, T](op: => Future[Either[E, T]]): IO[Either[HeatServiceError, E], T] =
    IO.async { callback: Callback[Either[HeatServiceError, E], T] =>
      op onComplete {
        case Success(Left(e)) => callback(Failed(Right(e)))
        case Success(Right(v)) => callback(Completed(v))
        case Failure(t) => callback(Failed(Left(HeatServiceError(t))))
      }
    }
}
