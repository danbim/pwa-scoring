package com.bimschas.pwascoring.domain

import com.bimschas.pwascoring.domain.Contest.ContestAlreadyPlanned
import com.bimschas.pwascoring.domain.Contest.ContestNotPlanned

import scala.collection.immutable.TreeSet

final case class Contest(private val plannedHeats: Option[TreeSet[HeatId]]) {

  lazy val heats: Either[ContestNotPlanned.type, Set[HeatId]] =
    plannedHeats match {
      case None => Left(ContestNotPlanned)
      case Some(heatIds) => Right(heatIds)
    }

  def planContest(heatIds: Set[HeatId]): Either[ContestAlreadyPlanned.type, ContestPlannedEvent] = {
    if (plannedHeats.isDefined) {
      Left(ContestAlreadyPlanned)
    } else {
      Right(ContestPlannedEvent(heatIds))
    }
  }

  def handleEvent(contestEvent: ContestEvent): Contest = {
    contestEvent match {
      case ContestPlannedEvent(heatIds) => Contest(plannedHeats = Some(TreeSet.empty[HeatId] ++ heatIds))
    }
  }
}

object Contest {

  final case object ContestNotPlanned
  final case object ContestAlreadyPlanned
  final case class HeatIdUnknown(heatId: HeatId)

  def empty: Contest =
    Contest(plannedHeats = None)
}
