package com.bimschas.pwascoring.domain

import com.bimschas.pwascoring.domain.Contest.HeatAlreadyStarted

final case class Contest(heats: Set[HeatId]) {

  def startHeat(heatId: HeatId, contestants: HeatContestants): Either[HeatAlreadyStarted, HeatStartedEvent] = {
    if (heats.contains(heatId)) {
      Left(HeatAlreadyStarted(heatId))
    } else {
      Right(HeatStartedEvent(heatId))
    }
  }

  def handleEvent(contestEvent: ContestEvent): Contest = {
    contestEvent match {
      case HeatStartedEvent(heatId) => Contest(heats + heatId)
    }
  }
}

object Contest {

  sealed trait Errors
  final case class HeatAlreadyStarted(heatId: HeatId) extends Errors
  final case class HeatIdUnknown(heatId: HeatId) extends Errors

  def handleEvent(contest: Contest, contestEvent: ContestEvent): Contest =
    contest.handleEvent(contestEvent)

  def empty: Contest =
    Contest(Set.empty)
}
