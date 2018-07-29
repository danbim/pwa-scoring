package com.bimschas.pwascoring.domain

sealed trait ContestEvent
final case class ContestPlannedEvent(heatIds: Set[HeatId]) extends ContestEvent