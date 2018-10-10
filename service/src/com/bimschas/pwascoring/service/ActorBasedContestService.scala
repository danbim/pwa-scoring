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
import com.bimschas.pwascoring.service.Service.ServiceError
import scalaz.zio.IO

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
  private implicit val timeout: Timeout = Timeout(30.seconds)

  override protected implicit val ec: ExecutionContext = system.executionContext

  override def planContest(heatIds: Set[HeatId]): IO[Either[ServiceError, Contest.ContestAlreadyPlanned.type], ContestPlannedEvent] =
    io(contestActor ? (ref => PlanContest(heatIds, ref)))

  override def heats(): IO[Either[ServiceError, ContestNotPlanned.type], Set[HeatId]] =
    io(contestActor ? (ref => GetHeats(ref)))

  override def heat(heatId: HeatId): IO[Either[ServiceError, HeatIdUnknown], HeatService] = {
    val future: Future[Either[HeatIdUnknown, EntityRef[HeatCommand]]] = contestActor ? (ref => GetHeat(heatId, ref))
    io(future.map(_.right.map(ActorBasedHeatService(system)(_))))
  }
}