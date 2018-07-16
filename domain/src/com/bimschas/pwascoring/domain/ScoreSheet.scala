package com.bimschas.pwascoring.domain

final case class ScoreSheets(scoreSheetsByRider: Map[RiderId, ScoreSheet]) {

  def score(riderId: RiderId, waveScore: WaveScore): ScoreSheets =
      copy(scoreSheetsByRider = scoreSheetsByRider + (riderId -> (scoreSheetsByRider.getOrElse(riderId, ScoreSheet.empty) + waveScore)))

  def score(riderId: RiderId, jumpScore: JumpScore): ScoreSheets =
      copy(scoreSheetsByRider = scoreSheetsByRider + (riderId -> (scoreSheetsByRider.getOrElse(riderId, ScoreSheet.empty) + jumpScore)))
}

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
      case (countingJumpScores, _) if countingJumpScores.size >= rules.jumpsCounting =>
        countingJumpScores
      case (countingJumpScores, jumpScore) if countingJumpScores.map(_.jumpType).contains(jumpScore.jumpType) =>
        countingJumpScores
      case (countingJumpScores, jumpScore) =>
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
