package com.bimschas.pwascoring.domain

import scala.util.Failure
import scala.util.Success
import scala.util.Try

final case class HeatId(number: Int, classifier: Option[String]) {
  override def toString: String = classifier match {
    case Some(c) => s"$number-$c"
    case None => s"$number"
  }
}

object HeatId {

  implicit val ordering: Ordering[HeatId] =
    Ordering.Tuple2(Ordering.Int, Ordering.String).on(heatId => (heatId.number, heatId.classifier.getOrElse(" ")))

  def parse(heatIdString: String): Try[HeatId] = {
    heatIdString.split("-").toList match {
      case number :: classifier :: Nil => Success(HeatId(number.toInt, Some(classifier)))
      case number :: Nil => Success(HeatId(number.toInt, None))
      case _ => Failure(new IllegalArgumentException(s"$heatIdString is not a valid HeatId"))
    }
  }
}

