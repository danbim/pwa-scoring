package com.bimschas.pwascoring.domain

import org.scalacheck.Gen
import org.scalatest.Matchers
import org.scalatest.WordSpecLike
import org.scalatest.prop.PropertyChecks

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

      case class ScoreSheetInput(
        heatRules: HeatRules,
        countingWaveScores: List[WaveScore],
        countingJumpScores: List[JumpScore],
        nonCountingWaveScores: List[WaveScore],
        nonCountingJumpScores: List[JumpScore]
      )

      val scoreSheetInputGen: Gen[ScoreSheetInput] = for {
        heatRules <- heatRulesGen
        countingWaveScores <- Gen.const(List.fill(heatRules.wavesCounting)(sample(waveScoreGen()))) // TODO fixed size? or shortListGen.take(heatRules.countingJumps)
        countingJumpScores <- Gen.const(Random.shuffle(JumpType.values.toList)  // TODO fixed size? or shortListGen.take(heatRules.countingJumps)
          .take(heatRules.jumpsCounting)
          .map(jumpType => sample(jumpScoreGen(jumpTypeGen = jumpType)))
        )
        nonCountingWaveScores <-
          if (heatRules.wavesCounting > 0) {
            shortListGen(waveScoreGen(pointsGen = pointsGen(max = countingWaveScores.min.points)))
          } else {
            Gen.const(Nil)
          }
        nonCountingJumpScores <-
          if (heatRules.jumpsCounting > 0) {
            shortListGen(jumpScoreGen(
              jumpTypeGen = Gen.oneOf(countingJumpScores.map(_.jumpType)),
              pointsGen = pointsGen(max = countingJumpScores.min.points)
            ))
          } else {
            Gen.const(Nil)
          }
      } yield ScoreSheetInput(heatRules, countingWaveScores, countingJumpScores, nonCountingWaveScores, nonCountingJumpScores)

      forAll(scoreSheetInputGen) { case ScoreSheetInput(heatRules, countingWaveScores, countingJumpScores, nonCountingWaveScores, nonCountingJumpScores) =>

        countingJumpScores.map(_.jumpType).toSet should have size countingJumpScores.size // only one scoring jump per jumpType allowed

        if (nonCountingWaveScores.nonEmpty) nonCountingWaveScores.max should be <= countingWaveScores.min
        if (nonCountingJumpScores.nonEmpty) nonCountingJumpScores.max should be <= countingJumpScores.min

        val waveScores = Random.shuffle(countingWaveScores ++ nonCountingWaveScores)
        val jumpScores = Random.shuffle(countingJumpScores ++ nonCountingJumpScores)

        val countingWavePoints = countingWaveScores.map(_.points.value).sum
        val countingJumpPoints = countingJumpScores.map(_.points.value).sum

        val scoreSheet = ScoreSheet(waveScores, jumpScores)

        scoreSheet.countingJumpScores(heatRules) should contain theSameElementsAs countingJumpScores
        scoreSheet.totalJumpScore(heatRules).value shouldBe countingJumpPoints

        scoreSheet.countingWaveScores(heatRules) should contain theSameElementsAs countingWaveScores
        scoreSheet.totalWaveScore(heatRules).value shouldBe countingWavePoints

        scoreSheet.totalScore(heatRules).value shouldBe countingWavePoints + countingJumpPoints
      }
    }
  }
}
