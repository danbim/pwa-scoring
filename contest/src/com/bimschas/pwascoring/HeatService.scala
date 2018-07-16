package com.bimschas.pwascoring

import akka.actor.Scheduler
import akka.cluster.sharding.typed.scaladsl.EntityRef
import akka.util.Timeout
import com.bimschas.pwascoring.HeatActor.GetContestants
import com.bimschas.pwascoring.HeatActor.GetScoreSheets
import com.bimschas.pwascoring.HeatActor.HeatCommand
import com.bimschas.pwascoring.HeatActor.ScoreWave
import com.bimschas.pwascoring.HeatActor.WaveScored
import com.bimschas.pwascoring.domain.Heat.UnknownRiderId
import com.bimschas.pwascoring.domain.HeatContestants
import com.bimschas.pwascoring.domain.RiderId
import com.bimschas.pwascoring.domain.ScoreSheet
import com.bimschas.pwascoring.domain.ScoreSheets
import com.bimschas.pwascoring.domain.WaveScore

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

trait HeatService {

  def contestants: Future[HeatContestants]
  def scoreSheets: Future[ScoreSheets]
  def score(riderId: RiderId, waveScore: WaveScore): Future[Either[UnknownRiderId, WaveScored]]
}

case class ActorBasedHeatService(heatEntity: EntityRef[HeatCommand])(implicit scheduler: Scheduler, ec: ExecutionContext) extends HeatService {
  private implicit val timeout: Timeout = Timeout(30.seconds)

  override def contestants: Future[HeatContestants] =
    heatEntity ? (ref => GetContestants(ref))

  override def scoreSheets: Future[ScoreSheets] =
    heatEntity ? (ref => GetScoreSheets(ref))

  override def score(riderId: RiderId, waveScore: WaveScore): Future[Either[UnknownRiderId, WaveScored]] =
    heatEntity ? (ref => ScoreWave(riderId, waveScore, ref))
}
