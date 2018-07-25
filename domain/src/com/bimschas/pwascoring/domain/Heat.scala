package com.bimschas.pwascoring.domain

import com.bimschas.pwascoring.domain.Heat.RiderIdUnknown

final case class Heat(contestants: HeatContestants, scoreSheets: ScoreSheets) {

  def scoreJump(riderId: RiderId, jumpScore: JumpScore): Either[RiderIdUnknown, JumpScoredEvent] = {
    if (contestants.riderIds.contains(riderId))
      Right(JumpScoredEvent(riderId, jumpScore))
    else
      Left(RiderIdUnknown(riderId))
  }

  def scoreWave(riderId: RiderId, waveScore: WaveScore): Either[RiderIdUnknown, WaveScoredEvent] = {
    if (contestants.riderIds.contains(riderId))
      Right(WaveScoredEvent(riderId, waveScore))
    else
      Left(RiderIdUnknown(riderId))
  }

  def handleEvent(event: HeatEvent): Heat = {
    event match {
      case WaveScoredEvent(riderId, waveScore) => this + (riderId, waveScore)
      case JumpScoredEvent(riderId, jumpScore) => this + (riderId, jumpScore)
    }
  }

  private def +(riderId: RiderId, waveScore: WaveScore): Heat = {
    copy(scoreSheets = scoreSheets.score(riderId, waveScore))
  }

  private def +(riderId: RiderId, jumpScore: JumpScore): Heat = {
    copy(scoreSheets = scoreSheets.score(riderId, jumpScore))
  }
}

object Heat {

  sealed trait Errors
  final case class RiderIdUnknown(riderId: RiderId)

  def handleEvent(heat: Heat, event: HeatEvent): Heat =
    heat.handleEvent(event)

  def empty(contestants: HeatContestants): Heat =
    Heat(contestants, ScoreSheets(contestants.riderIds.map(_ -> ScoreSheet.empty).toMap))
}
