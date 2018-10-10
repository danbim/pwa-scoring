package com.bimschas.pwascoring.service

import akka.actor.Scheduler
import akka.actor.typed.ActorSystem
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
import com.bimschas.pwascoring.service.Service.ServiceError
import com.bimschas.pwascoring.service.HeatActor.EndHeat
import com.bimschas.pwascoring.service.HeatActor.GetContestants
import com.bimschas.pwascoring.service.HeatActor.GetScoreSheets
import com.bimschas.pwascoring.service.HeatActor.HeatCommand
import com.bimschas.pwascoring.service.HeatActor.PlanHeat
import com.bimschas.pwascoring.service.HeatActor.ScoreJump
import com.bimschas.pwascoring.service.HeatActor.ScoreWave
import com.bimschas.pwascoring.service.HeatActor.StartHeat
import scalaz.zio.IO

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt

case class ActorBasedHeatService(system: ActorSystem[_])(heatEntity: EntityRef[HeatCommand]) extends HeatService {

  private implicit val scheduler: Scheduler = system.scheduler
  private implicit val timeout: Timeout = Timeout(30.seconds)

  override protected implicit val ec: ExecutionContext = system.executionContext

  override def planHeat(contestants: HeatContestants, rules: HeatRules): IO[Either[ServiceError, PlanHeatError], HeatPlannedEvent] =
    io(heatEntity ? (ref => PlanHeat(contestants, rules, ref)))

  override def contestants(): IO[Either[ServiceError, HeatNotPlanned.type], HeatContestants] =
    io(heatEntity ? (ref => GetContestants(ref)))

  override def scoreSheets(): IO[Either[ServiceError, HeatNotPlanned.type], ScoreSheets] =
    io(heatEntity ? (ref => GetScoreSheets(ref)))

  override def startHeat(): IO[Either[ServiceError, StartHeatError], HeatStartedEvent] =
    io(heatEntity ? (ref => StartHeat(ref)))

  override def score(riderId: RiderId, waveScore: WaveScore): IO[Either[ServiceError, ScoreWaveError], WaveScoredEvent] =
    io(heatEntity ? (ref => ScoreWave(riderId, waveScore, ref)))

  override def score(riderId: RiderId, jumpScore: JumpScore): IO[Either[ServiceError, ScoreJumpError], JumpScoredEvent] =
    io(heatEntity ? (ref => ScoreJump(riderId, jumpScore, ref)))

  override def endHeat(): IO[Either[ServiceError, EndHeatError], HeatEndedEvent] =
    io(heatEntity ? (ref => EndHeat(ref)))

}
