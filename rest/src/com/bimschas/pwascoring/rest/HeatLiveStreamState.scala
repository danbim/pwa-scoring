package com.bimschas.pwascoring.rest

import com.bimschas.pwascoring.domain.Heat
import com.bimschas.pwascoring.domain.HeatId
import com.bimschas.pwascoring.domain.Points
import com.bimschas.pwascoring.domain.RiderId
import com.bimschas.pwascoring.domain.ScoreSheet

case class HeatLiveStreamState(
  heatId: HeatId,
  started: Boolean,
  ended: Boolean,
  scoreSheets: Option[Map[RiderId, ScoreSheet]],
  totalScores: Option[Map[RiderId, Points]],
  leader: Option[RiderId]
)

object HeatLiveStreamState {

  def apply(heat: Heat): HeatLiveStreamState = {
    val totalScores = toTotalScores(heat)
    HeatLiveStreamState(
      heatId = heat.heatId,
      started = heat.started,
      ended = heat.ended,
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
