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
import com.bimschas.pwascoring.service.Service.ServiceError
import scalaz.zio.IO

trait HeatService extends Service {
  //format: OFF
  def planHeat(contestants: HeatContestants, rules: HeatRules): IO[Either[ServiceError, PlanHeatError],       HeatPlannedEvent]
  def contestants():                                            IO[Either[ServiceError, HeatNotPlanned.type], HeatContestants]
  def scoreSheets():                                            IO[Either[ServiceError, HeatNotPlanned.type], ScoreSheets]
  def startHeat():                                              IO[Either[ServiceError, StartHeatError],      HeatStartedEvent]
  def score(riderId: RiderId, waveScore: WaveScore):            IO[Either[ServiceError, ScoreWaveError],      WaveScoredEvent]
  def score(riderId: RiderId, jumpScore: JumpScore):            IO[Either[ServiceError, ScoreJumpError],      JumpScoredEvent]
  def endHeat():                                                IO[Either[ServiceError, EndHeatError],        HeatEndedEvent]
  //format: ON
}

