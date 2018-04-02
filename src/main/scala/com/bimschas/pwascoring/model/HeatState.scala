package com.bimschas.pwascoring.model

final case class HeatState(scoreSheets: Map[RiderId, RiderScoreSheet]) {

  def +(waveScore: WaveScore): HeatState = {
    copy(scoreSheets = scoreSheets + (waveScore.riderId -> (scoreSheets.getOrElse(waveScore.riderId, RiderScoreSheet.empty) + waveScore)))
  }

  def +(jumpScore: JumpScore): HeatState = {
    copy(scoreSheets = scoreSheets + (jumpScore.riderId -> (scoreSheets.getOrElse(jumpScore.riderId, RiderScoreSheet.empty) + jumpScore)))
  }
}

object HeatState {

  def empty(contestants: HeatContestants): HeatState =
    HeatState(Map(
      contestants.riderA -> RiderScoreSheet.empty,
      contestants.riderB -> RiderScoreSheet.empty
    ))
}
