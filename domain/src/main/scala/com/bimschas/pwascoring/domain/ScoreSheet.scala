package com.bimschas.pwascoring.domain

final case class ScoreSheet(waveScores: List[WaveScore], jumpScores: List[JumpScore]) {

  def +(waveScore: WaveScore): ScoreSheet = copy(waveScores = waveScores :+ waveScore)
  def +(jumpScore: JumpScore): ScoreSheet = copy(jumpScores = jumpScores :+ jumpScore)

  def countingWaveScores(rules: HeatRules): List[WaveScore] = {
    waveScores.sorted(WaveScore.DescendingOrdering).take(rules.wavesCounting)
  }

  def totalWaveScore(rules: HeatRules): Points = {
    Points(countingWaveScores(rules).map(_.points.value).sum)
  }

  def countingJumpScores(rules: HeatRules): List[JumpScore] = {
    jumpScores.sorted(Score.DescendingOrdering).foldLeft(List.empty[JumpScore]) {
      case (countingJumpScores, jumpScore) =>
        if (countingJumpScores.size >= rules.jumpsCounting)
          countingJumpScores
        else if (countingJumpScores.map(_.jumpType).contains(jumpScore.jumpType))
          countingJumpScores
        else
          countingJumpScores ++ List(jumpScore)
    }
  }

  def totalJumpScore(rules: HeatRules): Points = {
    Points(countingJumpScores(rules).map(_.points.value).sum)
  }

  def totalScore(rules: HeatRules): Points =
    totalWaveScore(rules) + totalJumpScore(rules)
}

object ScoreSheet {
  def empty: ScoreSheet = ScoreSheet(List.empty, List.empty)
}
