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

import scala.concurrent.Future

trait HeatService {
  def planHeat(contestants: HeatContestants, rules: HeatRules): Future[Either[PlanHeatError, HeatPlannedEvent]]
  def contestants(): Future[Either[HeatNotPlanned.type, HeatContestants]]
  def scoreSheets(): Future[Either[HeatNotPlanned.type, ScoreSheets]]
  def startHeat(): Future[Either[StartHeatError, HeatStartedEvent]]
  def score(riderId: RiderId, waveScore: WaveScore): Future[Either[ScoreWaveError, WaveScoredEvent]]
  def score(riderId: RiderId, jumpScore: JumpScore): Future[Either[ScoreJumpError, JumpScoredEvent]]
  def endHeat(): Future[Either[EndHeatError, HeatEndedEvent]]
}
