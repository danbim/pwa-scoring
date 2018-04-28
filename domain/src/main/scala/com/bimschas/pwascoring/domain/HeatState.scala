package com.bimschas.pwascoring.domain

final case class HeatState(contestants: HeatContestants, scoreSheets: Map[RiderId, ScoreSheet]) {

  def +(riderId: RiderId, waveScore: WaveScore): HeatState = {
    copy(scoreSheets = scoreSheets + (riderId -> (scoreSheets.getOrElse(riderId, ScoreSheet.empty) + waveScore)))
  }

  def +(riderId: RiderId, jumpScore: JumpScore): HeatState = {
    copy(scoreSheets = scoreSheets + (riderId -> (scoreSheets.getOrElse(riderId, ScoreSheet.empty) + jumpScore)))
  }
}

object HeatState {

  def empty(contestants: HeatContestants): HeatState =
    HeatState(contestants, contestants.riderIds.map(_ -> ScoreSheet.empty).toMap)
}
