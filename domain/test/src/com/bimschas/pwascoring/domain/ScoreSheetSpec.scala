package com.bimschas.pwascoring.domain

import org.scalacheck.Gen
import org.scalatest.Matchers
import org.scalatest.WordSpecLike
import org.scalatest.prop.PropertyChecks

import scala.collection.immutable.List
import scala.language.implicitConversions
import scala.util.Random

class ScoreSheetSpec extends WordSpecLike with Matchers with PropertyChecks with Generators {

  implicit val waveOrdering: Ordering[WaveScore] = WaveScore.AscendingOrdering
  implicit val jumpOrdering: Ordering[JumpScore] = JumpScore.AscendingOrdering

  case class ScoreSheetInput(
    heatRules: HeatRules,
    countingWaveScores: List[WaveScore],
    countingJumpScores: List[JumpScore],
    nonCountingWaveScores: List[WaveScore],
    nonCountingJumpScores: List[JumpScore]
  )

  val scoreSheetInputGen: Gen[ScoreSheetInput] = for {
    heatRules <- heatRulesGen
    countingWaveScores <- fixedSizeListGen(heatRules.wavesCounting, waveScoreGen())
    countingJumpScores <- Gen.const(
      Random.shuffle(JumpType.values.toList)
        .take(heatRules.jumpsCounting)
        .map(jumpType => sample(jumpScoreGen(jumpTypeGen = jumpType)))
    )
    nonCountingWaveScores <-
      if (countingWaveScores.nonEmpty) {
        shortListGen(waveScoreGen(pointsGen = pointsGen(max = countingWaveScores.min.points)))
      } else {
        Gen.const(Nil)
      }
    nonCountingJumpScores <-
      if (countingJumpScores.nonEmpty) {
        shortListGen(jumpScoreGen(
          jumpTypeGen = Gen.oneOf(countingJumpScores.map(_.jumpType)),
          pointsGen = pointsGen(max = countingJumpScores.min.points)
        ))
      } else {
        Gen.const(Nil)
      }
  } yield ScoreSheetInput(heatRules, countingWaveScores, countingJumpScores, nonCountingWaveScores, nonCountingJumpScores)

  "ScoreSheet" must {
    "calculate effective score (non-empty)" in {

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
