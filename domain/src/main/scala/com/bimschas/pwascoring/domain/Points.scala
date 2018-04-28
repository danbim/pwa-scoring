package com.bimschas.pwascoring.domain

import java.math.MathContext

import scala.language.implicitConversions
import scala.math.BigDecimal.RoundingMode

case class Points private (value: BigDecimal) extends AnyVal {
  def +(other: Points): Points = Points(value + other.value)
}

object Points {

  val Min = Points(toBigDecimal(0.0))
  val Max = Points(toBigDecimal(12.0))

  val AscendingOrdering: Ordering[Points] = Ordering.by(_.value)
  val DescendingOrdering: Ordering[Points] = AscendingOrdering.reverse

  def apply(value: Double): Points = {
    require(value >= Min.value && value <= Max.value) // rly, let's be realistic!
    Points(toBigDecimal(value))
  }

  private val digits = 1
  private def toBigDecimal(value: Double): BigDecimal = {
    BigDecimal(value, MathContext.UNLIMITED).setScale(digits, RoundingMode.CEILING)
  }
}
