package com.bimschas.pwascoring.domain

sealed trait ContestEvent
final case class HeatStartedEvent(heatId: HeatId) extends ContestEvent
