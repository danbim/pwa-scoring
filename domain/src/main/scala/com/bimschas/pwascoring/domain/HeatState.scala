package com.bimschas.pwascoring.domain

final case class HeatState(scoreSheets: Map[RiderId, ScoreSheet]) {

  def +(waveScore: WaveScore): HeatState = {
    copy(scoreSheets = scoreSheets + (waveScore.riderId -> (scoreSheets.getOrElse(waveScore.riderId, ScoreSheet.empty) + waveScore)))
  }

  def +(jumpScore: JumpScore): HeatState = {
    copy(scoreSheets = scoreSheets + (jumpScore.riderId -> (scoreSheets.getOrElse(jumpScore.riderId, ScoreSheet.empty) + jumpScore)))
  }
}

object HeatState {

  def empty(contestants: HeatContestants): HeatState =
    HeatState(Map(
      contestants.riderA -> ScoreSheet.empty,
      contestants.riderB -> ScoreSheet.empty
    ))
}
