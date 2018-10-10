package com.bimschas.pwascoring.service

import com.bimschas.pwascoring.domain.Contest.ContestAlreadyPlanned
import com.bimschas.pwascoring.domain.Contest.ContestNotPlanned
import com.bimschas.pwascoring.domain.Contest.HeatIdUnknown
import com.bimschas.pwascoring.domain.ContestPlannedEvent
import com.bimschas.pwascoring.domain.HeatId
import com.bimschas.pwascoring.service.Service.ServiceError
import scalaz.zio.IO

trait ContestService extends Service {
  //format: OFF
  def planContest(heatIds: Set[HeatId]): IO[Either[ServiceError, ContestAlreadyPlanned.type], ContestPlannedEvent]
  def heats():                           IO[Either[ServiceError, ContestNotPlanned.type],     Set[HeatId]]
  def heat(heatId: HeatId):              IO[Either[ServiceError, HeatIdUnknown],              HeatService]
  //format: ON
}
