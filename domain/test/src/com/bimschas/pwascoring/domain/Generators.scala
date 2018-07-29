package com.bimschas.pwascoring.domain

import org.scalacheck.Gen
import org.scalacheck.Gen.nonEmptyContainerOf
import org.scalacheck.Gen.resize

import scala.collection.immutable.List

trait Generators {

  def sample[A](g: Gen[A]): A =
    Stream.fill(30)(g.sample)
      .collectFirst { case Some(a) => a }
      .getOrElse(sys.error("Generator failed to generate element"))

  def fixedSizeListGen[A](size: Int, g: Gen[A]): Gen[List[A]] =
    List.fill(size)(sample(g))

  def shortListGen[A](g: Gen[A]): Gen[List[A]] =
    shortListGen(10, g)

  def shortListGen[A](maxSize: Int, g: Gen[A]): Gen[List[A]] =
    Gen.resize(maxSize, Gen.listOf(g))

  def nonEmptyListGen[A](maxSize: Int, g: Gen[A]): Gen[List[A]] =
    Gen.resize(maxSize, Gen.nonEmptyListOf(g))

  def nonEmptyShortListGen[A](g: Gen[A]): Gen[List[A]] =
    nonEmptyListGen(10, g)

  def nonEmptySetGen[A](maxSize: Int, g: Gen[A]): Gen[Set[A]] =
    resize(maxSize, nonEmptyContainerOf[Set, A](g))

  def nonEmptySmallSetGen[A](g: Gen[A]): Gen[Set[A]] =
    nonEmptySetGen(10, g)

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
    Gen.zip(Gen.chooseNum(0, 3), Gen.chooseNum(0, 5))
      .suchThat { case (wavesCounting, jumpsCounting) => wavesCounting > 0 && jumpsCounting > 0 }
      .map { case (wavesCounting, jumpsCounting) => HeatRules(wavesCounting, jumpsCounting) }
}
