package com.bimschas.pwascoring

import akka.actor.Scheduler
import akka.cluster.sharding.typed.scaladsl.EntityRef
import akka.util.Timeout
import com.bimschas.pwascoring.HeatActor.EndHeat
import com.bimschas.pwascoring.HeatActor.GetContestants
import com.bimschas.pwascoring.HeatActor.GetScoreSheets
import com.bimschas.pwascoring.HeatActor.HeatCommand
import com.bimschas.pwascoring.HeatActor.PlanHeat
import com.bimschas.pwascoring.HeatActor.ScoreJump
import com.bimschas.pwascoring.HeatActor.ScoreWave
import com.bimschas.pwascoring.HeatActor.StartHeat
import com.bimschas.pwascoring.domain.Heat.EndHeatError
import com.bimschas.pwascoring.domain.Heat.HeatNotPlanned
import com.bimschas.pwascoring.domain.Heat.PlanHeatError
import com.bimschas.pwascoring.domain.Heat.RiderIdUnknown
import com.bimschas.pwascoring.domain.Heat.ScoreJumpError
import com.bimschas.pwascoring.domain.Heat.ScoreWaveError
import com.bimschas.pwascoring.domain.Heat.StartHeatError
import com.bimschas.pwascoring.domain.HeatContestants
import com.bimschas.pwascoring.domain.HeatEndedEvent
import com.bimschas.pwascoring.domain.HeatPlannedEvent
import com.bimschas.pwascoring.domain.HeatStartedEvent
import com.bimschas.pwascoring.domain.JumpScore
import com.bimschas.pwascoring.domain.JumpScoredEvent
import com.bimschas.pwascoring.domain.RiderId
import com.bimschas.pwascoring.domain.ScoreSheets
import com.bimschas.pwascoring.domain.WaveScore
import com.bimschas.pwascoring.domain.WaveScoredEvent

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

trait HeatService {
  def planHeat(contestants: HeatContestants): Future[Either[PlanHeatError, HeatPlannedEvent]]
  def contestants(): Future[Either[HeatNotPlanned.type, HeatContestants]]
  def scoreSheets(): Future[Either[HeatNotPlanned.type, ScoreSheets]]
  def startHeat(): Future[Either[StartHeatError, HeatStartedEvent]]
  def score(riderId: RiderId, waveScore: WaveScore): Future[Either[ScoreWaveError, WaveScoredEvent]]
  def score(riderId: RiderId, jumpScore: JumpScore): Future[Either[ScoreJumpError, JumpScoredEvent]]
  def endHeat(): Future[Either[EndHeatError, HeatEndedEvent]]
}

case class ActorBasedHeatService(heatEntity: EntityRef[HeatCommand])(implicit scheduler: Scheduler, ec: ExecutionContext) extends HeatService {
  private implicit val timeout: Timeout = Timeout(30.seconds)

  override def planHeat(contestants: HeatContestants): Future[Either[PlanHeatError, HeatPlannedEvent]] =
    heatEntity ? (ref => PlanHeat(contestants, ref))

  override def contestants(): Future[Either[HeatNotPlanned.type, HeatContestants]] =
    heatEntity ? (ref => GetContestants(ref))

  override def scoreSheets(): Future[Either[HeatNotPlanned.type, ScoreSheets]] =
    heatEntity ? (ref => GetScoreSheets(ref))

  override def startHeat(): Future[Either[StartHeatError, HeatStartedEvent]] =
    heatEntity ? (ref => StartHeat(ref))

  override def score(riderId: RiderId, waveScore: WaveScore): Future[Either[ScoreWaveError, WaveScoredEvent]] =
    heatEntity ? (ref => ScoreWave(riderId, waveScore, ref))

  override def score(riderId: RiderId, jumpScore: JumpScore): Future[Either[ScoreJumpError, JumpScoredEvent]] =
    heatEntity ? (ref => ScoreJump(riderId, jumpScore, ref))

  override def endHeat(): Future[Either[EndHeatError, HeatEndedEvent]] =
    heatEntity ? (ref => EndHeat(ref))
}
