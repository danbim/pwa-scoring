package com.bimschas.pwascoring.domain

import com.bimschas.pwascoring.domain.Heat.EndHeatError
import com.bimschas.pwascoring.domain.Heat.HeatAlreadyEnded
import com.bimschas.pwascoring.domain.Heat.HeatAlreadyPlanned
import com.bimschas.pwascoring.domain.Heat.HeatAlreadyStarted
import com.bimschas.pwascoring.domain.Heat.HeatNotPlanned
import com.bimschas.pwascoring.domain.Heat.HeatNotStarted
import com.bimschas.pwascoring.domain.Heat.PlanHeatError
import com.bimschas.pwascoring.domain.Heat.RiderIdUnknown
import com.bimschas.pwascoring.domain.Heat.ScoreJumpError
import com.bimschas.pwascoring.domain.Heat.ScoreWaveError
import com.bimschas.pwascoring.domain.Heat.StartHeatError

final case class Heat(
  heatId: HeatId,
  scoreSheets: Option[ScoreSheets],
  started: Boolean,
  ended: Boolean
) {

  def planHeat(contestants: HeatContestants): Either[PlanHeatError, HeatPlannedEvent] = {
    if (scoreSheets.isDefined) Left(HeatAlreadyPlanned)
    else Right(HeatPlannedEvent(heatId, contestants))
  }

  def startHeat(): Either[StartHeatError, HeatStartedEvent] = {
    if (scoreSheets.isEmpty) Left(HeatNotPlanned)
    else if (started) Left(HeatAlreadyStarted)
    else Right(HeatStartedEvent(heatId))
  }

  def scoreJump(riderId: RiderId, jumpScore: JumpScore): Either[ScoreJumpError, JumpScoredEvent] = {
    if (!started) Left(HeatNotStarted)
    else if (ended) Left(HeatAlreadyEnded)
    else if (!scoreSheets.exists(_.scoreSheetsByRider.contains(riderId))) Left(RiderIdUnknown(riderId))
    else Right(JumpScoredEvent(heatId, riderId, jumpScore))
  }

  def scoreWave(riderId: RiderId, waveScore: WaveScore): Either[ScoreWaveError, WaveScoredEvent] = {
    if (!started) Left(HeatNotStarted)
    else if (ended) Left(HeatAlreadyEnded)
    else if (!scoreSheets.exists(_.scoreSheetsByRider.contains(riderId))) Left(RiderIdUnknown(riderId))
    else Right(WaveScoredEvent(heatId, riderId, waveScore))
  }

  def endHeat(): Either[EndHeatError, HeatEndedEvent] = {
    if (!started) Left(HeatNotStarted)
    else if (ended) Left(HeatAlreadyEnded)
    else Right(HeatEndedEvent(heatId))
  }

  def handleEvent(event: HeatEvent): Heat = {
    event match {
      case HeatPlannedEvent(_, contestants) => copy(scoreSheets = Some(ScoreSheets(contestants)))
      case HeatStartedEvent(_) => copy(started = true)
      case JumpScoredEvent(_, riderId, jumpScore) => this + (riderId, jumpScore)
      case WaveScoredEvent(_, riderId, waveScore) => this + (riderId, waveScore)
      case HeatEndedEvent(_) => copy(ended = true)
    }
  }

  private def +(riderId: RiderId, waveScore: WaveScore): Heat =
    copy(scoreSheets = scoreSheets.map(_.score(riderId, waveScore)))

  private def +(riderId: RiderId, jumpScore: JumpScore): Heat =
    copy(scoreSheets = scoreSheets.map(_.score(riderId, jumpScore)))
}

object Heat {

  sealed trait PlanHeatError
  sealed trait StartHeatError
  sealed trait ScoreJumpError
  sealed trait ScoreWaveError
  sealed trait EndHeatError

  case object HeatAlreadyPlanned extends PlanHeatError
  case object HeatAlreadyStarted extends StartHeatError
  case object HeatAlreadyEnded extends EndHeatError with StartHeatError with ScoreJumpError with ScoreWaveError

  case object HeatNotPlanned extends StartHeatError
  case object HeatNotStarted extends ScoreJumpError with ScoreWaveError with EndHeatError

  case class RiderIdUnknown(riderId: RiderId) extends ScoreJumpError with ScoreWaveError

  def apply(heatId: HeatId): Heat =
    Heat(heatId = heatId, scoreSheets = None, started = false, ended = false)
}
