package com.bimschas.pwascoring.domain

import org.scalacheck.Gen
import org.scalacheck.Gen.nonEmptyContainerOf
import org.scalacheck.Gen.resize

import scala.collection.immutable.List

object BasicGenerators extends BasicGenerators

trait BasicGenerators {

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
}
