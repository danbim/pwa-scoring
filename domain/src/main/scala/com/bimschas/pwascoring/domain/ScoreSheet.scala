package com.bimschas.pwascoring.domain

final case class ScoreSheet(waveScores: List[WaveScore], jumpScores: List[JumpScore]) {
  def +(waveScore: WaveScore): ScoreSheet = copy(waveScores = waveScores :+ waveScore)
  def +(jumpScore: JumpScore): ScoreSheet = copy(jumpScores = jumpScores :+ jumpScore)
}

object ScoreSheet {
  def empty: ScoreSheet = ScoreSheet(List.empty, List.empty)
}
