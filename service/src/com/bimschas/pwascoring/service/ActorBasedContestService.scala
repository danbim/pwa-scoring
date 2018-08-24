package com.bimschas.pwascoring.service

import akka.actor.Scheduler
import akka.actor.typed.ActorRef
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.scaladsl.adapter._
import akka.cluster.sharding.typed.scaladsl.EntityRef
import akka.util.Timeout
import akka.{actor => untyped}
import com.bimschas.pwascoring.domain.Contest
import com.bimschas.pwascoring.domain.Contest.ContestNotPlanned
import com.bimschas.pwascoring.domain.Contest.HeatIdUnknown
import com.bimschas.pwascoring.domain.ContestPlannedEvent
import com.bimschas.pwascoring.domain.HeatId
import com.bimschas.pwascoring.service.ContestActor.ContestCommand
import com.bimschas.pwascoring.service.ContestActor.GetHeat
import com.bimschas.pwascoring.service.ContestActor.GetHeats
import com.bimschas.pwascoring.service.ContestActor.PlanContest
import com.bimschas.pwascoring.service.HeatActor.HeatCommand

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

object ActorBasedContestService {

  def apply(system: ActorSystem[_]): ActorBasedContestService =
    ActorBasedContestService(system, ContestActor(system))

  def apply(system: untyped.ActorSystem): ActorBasedContestService =
    apply(system.toTyped)
}

case class ActorBasedContestService(system: ActorSystem[_], contestActor: ActorRef[ContestCommand])
  extends ContestService {

  private implicit val scheduler: Scheduler = system.scheduler
  private implicit val executionContext: ExecutionContext = system.executionContext
  private implicit val timeout: Timeout = Timeout(30.seconds)

  override def planContest(heatIds: Set[HeatId]): Future[Either[Contest.ContestAlreadyPlanned.type, ContestPlannedEvent]] =
    contestActor ? (ref => PlanContest(heatIds, ref))

  override def heats(): Future[Either[ContestNotPlanned.type, Set[HeatId]]] =
    contestActor ? (ref => GetHeats(ref))

  override def heat(heatId: HeatId): Future[Either[HeatIdUnknown, HeatService]] = {
    val future: Future[Either[HeatIdUnknown, EntityRef[HeatCommand]]] = contestActor ? (ref => GetHeat(heatId, ref))
    future.map(_.right.map(ActorBasedHeatService.apply))
  }
}