package com.bimschas.pwascoring

import akka.actor.Scheduler
import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.AskPattern._
import akka.cluster.sharding.typed.scaladsl.EntityRef
import akka.util.Timeout
import com.bimschas.pwascoring.ContestActor.ContestCommand
import com.bimschas.pwascoring.ContestActor.GetHeat
import com.bimschas.pwascoring.ContestActor.GetHeats
import com.bimschas.pwascoring.ContestActor.HeatStarted
import com.bimschas.pwascoring.ContestActor.StartHeat
import com.bimschas.pwascoring.HeatActor.HeatCommand
import com.bimschas.pwascoring.domain.Contest.HeatAlreadyStarted
import com.bimschas.pwascoring.domain.Contest.HeatIdUnknown
import com.bimschas.pwascoring.domain.HeatContestants
import com.bimschas.pwascoring.domain.HeatId

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

trait ContestService {

  def startHeat(heatId: HeatId, contestants: HeatContestants): Future[Either[HeatAlreadyStarted, HeatStarted]]
  def heats(): Future[Set[HeatId]]
  def heat(heatId: HeatId): Future[HeatService]
}

case class ActorBasedContestService(contestActor: ActorRef[ContestCommand])(implicit val scheduler: Scheduler, ec: ExecutionContext) extends ContestService {
  private implicit val timeout: Timeout = Timeout(30.seconds)

  override def heat(heatId: HeatId): Future[HeatService] = {
    val future: Future[Either[HeatIdUnknown, EntityRef[HeatCommand]]] = contestActor ? (ref => GetHeat(heatId, ref))
    future flatMap {
      case Left(heatIdUnknown) => Future.failed(new RuntimeException(s"HeatId ${heatIdUnknown.heatId} unknown")) // TODO meh
      case Right(heatEntityRef) => Future.successful(ActorBasedHeatService(heatEntityRef))
    }
  }

  override def startHeat(heatId: HeatId, contestants: HeatContestants): Future[Either[HeatAlreadyStarted, HeatStarted]] = {
    contestActor ? (ref => StartHeat(heatId, contestants, ref))
  }

  override def heats(): Future[Set[HeatId]] = {
    contestActor ? (ref => GetHeats(ref))
  }
}
