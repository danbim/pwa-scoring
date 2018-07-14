package com.bimschas.pwascoring

import akka.actor.Scheduler
import akka.cluster.sharding.typed.scaladsl.EntityRef
import akka.util.Timeout
import com.bimschas.pwascoring.HeatActor.GetContestants
import com.bimschas.pwascoring.HeatActor.GetScoreSheets
import com.bimschas.pwascoring.HeatActor.HeatCommand
import com.bimschas.pwascoring.domain.HeatContestants
import com.bimschas.pwascoring.domain.RiderId
import com.bimschas.pwascoring.domain.ScoreSheet

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

trait HeatService {

  def contestants: Future[HeatContestants]
  def scoreSheets: Future[Map[RiderId, ScoreSheet]]
}

case class ActorBasedHeatService(heatEntity: EntityRef[HeatCommand])(implicit scheduler: Scheduler, ec: ExecutionContext) extends HeatService {
  private implicit val timeout: Timeout = Timeout(30.seconds)

  override def contestants: Future[HeatContestants] =
    heatEntity ? (ref => GetContestants(ref))

  override def scoreSheets: Future[Map[RiderId, ScoreSheet]] =
    heatEntity ? (ref => GetScoreSheets(ref))
}
