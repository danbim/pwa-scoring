package com.bimschas.pwascoring.rest

import com.bimschas.pwascoring.domain.Heat
import com.bimschas.pwascoring.domain.HeatId
import com.bimschas.pwascoring.domain.Points
import com.bimschas.pwascoring.domain.RiderId
import com.bimschas.pwascoring.domain.ScoreSheet

case class HeatLiveStreamRiderState(
  totalScore: Points,
  scoreSheet: ScoreSheet
)

case class HeatLiveStreamState(
  heatId: HeatId,
  started: Boolean,
  ended: Boolean,
  riderStates: Option[Map[RiderId, HeatLiveStreamRiderState]],
  leader: Option[RiderId]
)

object HeatLiveStreamState {

  def apply(heat: Heat): HeatLiveStreamState = {
    HeatLiveStreamState(
      heatId = heat.heatId,
      started = heat.started,
      ended = heat.ended,
      riderStates = riderStates(heat),
      leader = leader(heat)
    )
  }

  private def riderStates(heat: Heat): Option[Map[RiderId, HeatLiveStreamRiderState]] =
    (heat.scoreSheets, heat.rules) match {
      case (Some(scoreSheets), Some(rules)) =>
        Some(scoreSheets.scoreSheetsByRider.mapValues { scoreSheet =>
          HeatLiveStreamRiderState(
            totalScore = scoreSheet.totalScore(rules),
            scoreSheet = scoreSheet
          )
        })
      case _ =>
        None
    }

  private def leader(heat: Heat): Option[RiderId] = {
    def toTotalScores(heat: Heat): Option[Map[RiderId, Points]] =
      (heat.scoreSheets, heat.rules) match {
        case (Some(scoreSheets), Some(rules)) =>
          Some(scoreSheets.scoreSheetsByRider.mapValues(_.totalScore(rules)))
        case _ =>
          None
      }

    toTotalScores(heat).flatMap { scores =>
      scores.toList
        .sortBy { case (_, points) => points }(Points.DescendingOrdering)
        .headOption
        .map { case (riderId, _) => riderId }
    }
  }
}
