package com.bimschas.pwascoring.model

sealed trait Score
final case class WaveScore(riderId: RiderId, score: BigDecimal) extends Score
final case class JumpScore(riderId: RiderId, jumpType: JumpType, score: BigDecimal) extends Score
