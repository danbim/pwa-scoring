package com.bimschas.pwascoring.domain

sealed trait HeatEvent
final case class WaveScoredEvent(riderId: RiderId, waveScore: WaveScore) extends HeatEvent
final case class JumpScoredEvent(riderId: RiderId, jumpScore: JumpScore) extends HeatEvent
