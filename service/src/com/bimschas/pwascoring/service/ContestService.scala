package com.bimschas.pwascoring.service

import com.bimschas.pwascoring.domain.Contest.ContestAlreadyPlanned
import com.bimschas.pwascoring.domain.Contest.ContestNotPlanned
import com.bimschas.pwascoring.domain.Contest.HeatIdUnknown
import com.bimschas.pwascoring.domain.ContestPlannedEvent
import com.bimschas.pwascoring.domain.HeatId

import scala.concurrent.Future

trait ContestService {
  def planContest(heatIds: Set[HeatId]): Future[Either[ContestAlreadyPlanned.type, ContestPlannedEvent]]
  def heats(): Future[Either[ContestNotPlanned.type, Set[HeatId]]]
  def heat(heatId: HeatId): Future[Either[HeatIdUnknown, HeatService]]
}
