package com.bimschas.pwascoring.domain

final case class HeatContestants(riderIds: List[RiderId])

object HeatContestants {
  def apply(riderId: RiderId, moreRiderIds: RiderId*): HeatContestants =
    HeatContestants(riderId :: moreRiderIds.toList)
}
