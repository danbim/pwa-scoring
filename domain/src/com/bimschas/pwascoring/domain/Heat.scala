package com.bimschas.pwascoring.domain

import com.bimschas.pwascoring.domain.Heat.UnknownRiderId

final case class Heat(contestants: HeatContestants, scoreSheets: Map[RiderId, ScoreSheet]) {

  def scoreJump(riderId: RiderId, jumpScore: JumpScore): Either[UnknownRiderId, JumpScoredEvent] = {
    if (contestants.riderIds.contains(riderId))
      Right(JumpScoredEvent(riderId, jumpScore))
    else
      Left(UnknownRiderId(riderId))
  }

  def scoreWave(riderId: RiderId, waveScore: WaveScore): Either[UnknownRiderId, WaveScoredEvent] = {
    if (contestants.riderIds.contains(riderId))
      Right(WaveScoredEvent(riderId, waveScore))
    else
      Left(UnknownRiderId(riderId))
  }

  def handleEvent(event: HeatEvent): Heat = {
    event match {
      case WaveScoredEvent(riderId, waveScore) => this + (riderId, waveScore)
      case JumpScoredEvent(riderId, jumpScore) => this + (riderId, jumpScore)
    }
  }

  private def +(riderId: RiderId, waveScore: WaveScore): Heat = {
    copy(scoreSheets = scoreSheets + (riderId -> (scoreSheets.getOrElse(riderId, ScoreSheet.empty) + waveScore)))
  }

  private def +(riderId: RiderId, jumpScore: JumpScore): Heat = {
    copy(scoreSheets = scoreSheets + (riderId -> (scoreSheets.getOrElse(riderId, ScoreSheet.empty) + jumpScore)))
  }
}

object Heat {

  sealed trait Errors
  final case class UnknownRiderId(riderId: RiderId)

  def handleEvent(heat: Heat, event: HeatEvent): Heat =
    heat.handleEvent(event)

  def empty(contestants: HeatContestants): Heat =
    Heat(contestants, contestants.riderIds.map(_ -> ScoreSheet.empty).toMap)
}
