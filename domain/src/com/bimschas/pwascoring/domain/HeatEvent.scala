package com.bimschas.pwascoring.domain

sealed trait HeatEvent
final case class HeatPlannedEvent(heatId: HeatId, contestants: HeatContestants, rules: HeatRules) extends HeatEvent
final case class HeatStartedEvent(heatId: HeatId) extends HeatEvent
final case class WaveScoredEvent(heatId: HeatId, riderId: RiderId, waveScore: WaveScore) extends HeatEvent
final case class JumpScoredEvent(heatId: HeatId, riderId: RiderId, jumpScore: JumpScore) extends HeatEvent
final case class HeatEndedEvent(heatId: HeatId) extends HeatEvent
