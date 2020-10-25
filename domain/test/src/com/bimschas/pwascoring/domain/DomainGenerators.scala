package com.bimschas.pwascoring.domain

import org.scalacheck.Gen

object DomainGenerators extends DomainGenerators

trait DomainGenerators extends BasicGenerators {

  val heatIdGen: Gen[HeatId] =
    for {
      number <- Gen.delay(IdCounter.next())
      classifier <- Gen.some(Gen.oneOf("a", "b"))
    } yield HeatId(number, classifier)

  val riderIdGen: Gen[RiderId] =
    RiderId("G-123")

  def pointsGen(min: Points, max: Points): Gen[Points] =
    Gen.chooseNum(min.value.doubleValue(), max.value.doubleValue()).map(Points(_))

  def pointsGen(max: Points): Gen[Points] =
    pointsGen(Points.Min, max)

  val pointsGen: Gen[Points] =
    pointsGen(Points.Min, Points.Max)

  def waveScoreGen(pointsGen: Gen[Points] = pointsGen): Gen[WaveScore] =
    pointsGen.map(WaveScore(_))

  val jumpTypeGen: Gen[JumpType] =
    Gen.oneOf(JumpType.values.toSeq)

  def jumpScoreGen(jumpTypeGen: Gen[JumpType] = jumpTypeGen, pointsGen: Gen[Points] = pointsGen): Gen[JumpScore] =
    for {
      jumpType <- jumpTypeGen
      points <- pointsGen
    } yield JumpScore(jumpType, points)

  val heatRulesGen: Gen[HeatRules] =
    Gen
      .zip(Gen.chooseNum(0, 3), Gen.chooseNum(0, 5))
      .suchThat { case (wavesCounting, jumpsCounting) => wavesCounting > 0 && jumpsCounting > 0 } // TODO "> 0" is wrong
      .map { case (wavesCounting, jumpsCounting) => HeatRules(wavesCounting, jumpsCounting) }
}
