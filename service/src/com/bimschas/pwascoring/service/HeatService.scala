package com.bimschas.pwascoring.service

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
import com.bimschas.pwascoring.service.HeatService.HeatServiceError
import scalaz.zio.IO

import scala.util.control.NoStackTrace

trait HeatService {
  def planHeat(contestants: HeatContestants, rules: HeatRules): IO[Either[HeatServiceError, PlanHeatError], HeatPlannedEvent]
  def contestants(): IO[Either[HeatServiceError, HeatNotPlanned.type], HeatContestants]
  def scoreSheets(): IO[Either[HeatServiceError, HeatNotPlanned.type], ScoreSheets]
  def startHeat(): IO[Either[HeatServiceError, StartHeatError], HeatStartedEvent]
  def score(riderId: RiderId, waveScore: WaveScore): IO[Either[HeatServiceError, ScoreWaveError], WaveScoredEvent]
  def score(riderId: RiderId, jumpScore: JumpScore): IO[Either[HeatServiceError, ScoreJumpError], JumpScoredEvent]
  def endHeat(): IO[Either[HeatServiceError, EndHeatError], HeatEndedEvent]
}

object HeatService {
  case class HeatServiceError(cause: Throwable) extends Exception with NoStackTrace
}
