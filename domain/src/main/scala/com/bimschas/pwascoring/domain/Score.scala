package com.bimschas.pwascoring.domain

sealed trait Score {
  val points: Points
}

final case class JumpScore private(jumpType: JumpType, points: Points) extends Score
final case class WaveScore private(points: Points) extends Score

object Score {

  val AscendingOrdering: Ordering[Score] = Ordering.by[Score, Points](_.points)(Points.AscendingOrdering)
  val DescendingOrdering: Ordering[Score] = AscendingOrdering.reverse
}

object JumpScore {

  val AscendingOrdering: Ordering[JumpScore] = Ordering.by[JumpScore, Points](_.points)(Points.AscendingOrdering)
  val DescendingOrdering: Ordering[JumpScore] = AscendingOrdering.reverse

  def apply(jumpType: JumpType, value: Double): JumpScore = JumpScore(jumpType, Points(value))
}

object WaveScore {

  val AscendingOrdering: Ordering[WaveScore] = Ordering.by[WaveScore, Points](_.points)(Points.AscendingOrdering)
  val DescendingOrdering: Ordering[WaveScore] = AscendingOrdering.reverse

  def apply(value: Double): WaveScore = WaveScore(Points(value))
}