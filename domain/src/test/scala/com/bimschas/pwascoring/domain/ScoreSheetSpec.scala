package com.bimschas.pwascoring.domain

import org.scalatest.Matchers
import org.scalatest.OptionValues
import org.scalatest.WordSpecLike
import org.scalatest.prop.PropertyChecks
import org.scalacheck.Properties
import org.scalacheck.Prop.forAll
import org.scalacheck.Gen

import scala.collection.immutable.List
import scala.language.implicitConversions
import scala.util.Random

class ScoreSheetSpec extends WordSpecLike with Matchers with PropertyChecks {

  def sample[A](g: Gen[A]): A = Stream.fill(30)(g.sample).collectFirst { case Some(a) => a }
    .getOrElse(sys.error("Generator failed to generate element"))
  def shortListGen[A](g: Gen[A]): Gen[List[A]] = Gen.resize(10, Gen.listOf(g))
  def nonEmptyListGen[A](maxSize: Int, g: Gen[A]): Gen[List[A]] = Gen.resize(maxSize, Gen.nonEmptyListOf(g))
  def nonEmptyShortListGen[A](g: Gen[A]): Gen[List[A]] = nonEmptyListGen(10, g)

  val riderIdGen: Gen[RiderId] =
    RiderId("G-123")

  def pointsGen(min: Points, max: Points): Gen[Points] = Gen.chooseNum(min.value.doubleValue(), max.value.doubleValue()).map(Points(_))
  def pointsGen(max: Points): Gen[Points] = pointsGen(Points.Min, max)
  val pointsGen: Gen[Points] = pointsGen(Points.Min, Points.Max)

  def waveScoreGen(pointsGen: Gen[Points] = pointsGen): Gen[WaveScore] =
    pointsGen.map(WaveScore(_))

  val jumpTypeGen: Gen[JumpType] =
    Gen.oneOf(JumpType.values.toSeq)

  def jumpScoreGen(
    jumpTypeGen: Gen[JumpType] = jumpTypeGen,
    pointsGen: Gen[Points] = pointsGen
  ): Gen[JumpScore] = {
    for {
      jumpType <- jumpTypeGen
      points <- pointsGen
    } yield JumpScore(jumpType, points)
  }

  val heatRulesGen = Gen.zip(Gen.chooseNum(0, 3), Gen.chooseNum(0, 5)).suchThat { case (waves, jumps) => waves > 0 && jumps > 0 }.map {
    case (wavesCounting, jumpsCounting) =>
      HeatRules(wavesCounting, jumpsCounting)
  }

  implicit val waveOrdering: Ordering[WaveScore] = WaveScore.AscendingOrdering
  implicit val jumpOrdering: Ordering[JumpScore] = JumpScore.AscendingOrdering

  "ScoreSheet" must {
    "calculate effective score (non-empty)" in {

      forAll(heatRulesGen) { (rules: HeatRules) =>

        val countingWaveScores: List[WaveScore] = List.fill(rules.wavesCounting)(sample(waveScoreGen()))
        val countingJumpScores: List[JumpScore] = Random.shuffle(JumpType.values.toList)
          .take(rules.jumpsCounting)
          .map(jumpType => sample(jumpScoreGen(jumpTypeGen = jumpType)))

        countingJumpScores.map(_.jumpType).toSet should have size countingJumpScores.size // only one scoring jump per jumpType allowed

        val nonCountingWaveScores: List[WaveScore] = {
          if (rules.wavesCounting > 0) {
            sample(shortListGen(sample(waveScoreGen(
              pointsGen = pointsGen(max = countingWaveScores.min.points)
            ))))
          } else Nil
        }
        val nonCountingJumpScores: List[JumpScore] = {
          if (rules.jumpsCounting > 0) {
            sample(shortListGen(sample(jumpScoreGen(
              jumpTypeGen = Gen.oneOf(countingJumpScores.map(_.jumpType)),
              pointsGen = pointsGen(max = countingJumpScores.min.points)
            ))))
          } else Nil
        }

        if (nonCountingWaveScores.nonEmpty) nonCountingWaveScores.max should be <= countingWaveScores.min
        if (nonCountingJumpScores.nonEmpty) nonCountingJumpScores.max should be <= countingJumpScores.min

        val waveScores = Random.shuffle(countingWaveScores ++ nonCountingWaveScores)
        val jumpScores = Random.shuffle(countingJumpScores ++ nonCountingJumpScores)

        val countingWavePoints = countingWaveScores.map(_.points.value).sum
        val countingJumpPoints = countingJumpScores.map(_.points.value).sum

        val scoreSheet = ScoreSheet(waveScores, jumpScores)

        scoreSheet.countingJumpScores(rules) should contain theSameElementsAs countingJumpScores
        scoreSheet.totalJumpScore(rules).value shouldBe countingJumpPoints

        scoreSheet.countingWaveScores(rules) should contain theSameElementsAs countingWaveScores
        scoreSheet.totalWaveScore(rules).value shouldBe countingWavePoints

        scoreSheet.totalScore(rules).value shouldBe countingWavePoints + countingJumpPoints
      }
    }
  }
}
