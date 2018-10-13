package com.bimschas.pwascoring.rest

import com.bimschas.pwascoring.domain.Heat
import com.bimschas.pwascoring.domain.HeatId
import com.bimschas.pwascoring.domain.Points
import com.bimschas.pwascoring.domain.RiderId
import com.bimschas.pwascoring.domain.ScoreSheet

case class HeatState(
  heatId: HeatId,
  started: Boolean,
  ended: Boolean,
  riderIds: Option[Array[RiderId]],
  scoreSheets: Option[Map[RiderId, ScoreSheet]],
  totalScores: Option[Map[RiderId, Points]],
  leader: Option[RiderId]
)

object HeatState {

  def apply(heat: Heat): HeatState = {
    val totalScores = toTotalScores(heat)
    HeatState(
      heatId = heat.heatId,
      started = heat.started,
      ended = heat.ended,
      riderIds = heat.scoreSheets.map(_.scoreSheetsByRider.keys.toArray),
      scoreSheets = heat.scoreSheets.map(_.scoreSheetsByRider),
      totalScores = totalScores,
      leader = leader(totalScores)
    )
  }

  private def toTotalScores(heat: Heat): Option[Map[RiderId, Points]] =
    (heat.scoreSheets, heat.rules) match {
      case (Some(scoreSheets), Some(rules)) => Some(scoreSheets.scoreSheetsByRider.mapValues(_.totalScore(rules)))
      case _ => None
    }

  private def leader(totalScores: Option[Map[RiderId, Points]]): Option[RiderId] =
    totalScores.flatMap { scores =>
      scores.toList
        .sortBy { case (_, points) => points }(Points.DescendingOrdering)
        .headOption
        .map { case (riderId, _) => riderId }
    }
}
