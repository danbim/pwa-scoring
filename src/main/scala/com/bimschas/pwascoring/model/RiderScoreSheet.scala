package com.bimschas.pwascoring.model

final case class RiderScoreSheet(waveScores: List[WaveScore], jumpScores: List[JumpScore]) {
  def +(waveScore: WaveScore): RiderScoreSheet = copy(waveScores = waveScores :+ waveScore)
  def +(jumpScore: JumpScore): RiderScoreSheet = copy(jumpScores = jumpScores :+ jumpScore)
}

object RiderScoreSheet {
  def empty: RiderScoreSheet = RiderScoreSheet(List.empty, List.empty)
}
