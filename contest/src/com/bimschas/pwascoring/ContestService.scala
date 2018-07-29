package com.bimschas.pwascoring

import akka.{actor => untyped}
import akka.actor.Scheduler
import akka.actor.typed.scaladsl.adapter._
import akka.actor.typed.ActorRef
import akka.actor.typed.ActorSystem
import akka.actor.typed.Props
import akka.actor.typed.scaladsl.AskPattern._
import akka.cluster.sharding.typed.scaladsl.EntityRef
import akka.cluster.typed.ClusterSingleton
import akka.cluster.typed.ClusterSingletonSettings
import akka.util.Timeout
import com.bimschas.pwascoring.ContestActor.ContestCommand
import com.bimschas.pwascoring.ContestActor.GetHeat
import com.bimschas.pwascoring.ContestActor.GetHeats
import com.bimschas.pwascoring.ContestActor.PlanContest
import com.bimschas.pwascoring.HeatActor.HeatCommand
import com.bimschas.pwascoring.domain.Contest
import com.bimschas.pwascoring.domain.Contest.ContestAlreadyPlanned
import com.bimschas.pwascoring.domain.Contest.ContestNotPlanned
import com.bimschas.pwascoring.domain.Contest.HeatIdUnknown
import com.bimschas.pwascoring.domain.ContestPlannedEvent
import com.bimschas.pwascoring.domain.HeatId

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

trait ContestService {
  def planContest(heatIds: Set[HeatId]): Future[Either[ContestAlreadyPlanned.type, ContestPlannedEvent]]
  def heats(): Future[Either[ContestNotPlanned.type, Set[HeatId]]]
  def heat(heatId: HeatId): Future[Either[HeatIdUnknown, HeatService]]
}

object ActorBasedContestService {

  def apply(untypedSystem: untyped.ActorSystem)(
    implicit scheduler: Scheduler, ec: ExecutionContext
  ): ActorBasedContestService = {
    val system: ActorSystem[_] = untypedSystem.toTyped
    val singletonManager = ClusterSingleton(system)
    val contestActor = singletonManager.spawn(
      behavior = ContestActor.behavior,
      "ContestActor",
      Props.empty,
      ClusterSingletonSettings(system),
      ContestActor.PassivateContest
    )
    ActorBasedContestService(contestActor)
  }
}

case class ActorBasedContestService(contestActor: ActorRef[ContestCommand])(
  implicit val scheduler: Scheduler, ec: ExecutionContext
) extends ContestService {

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
