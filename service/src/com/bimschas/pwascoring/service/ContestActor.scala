package com.bimschas.pwascoring.service

import akka.actor.typed.ActorRef
import akka.actor.typed.ActorSystem
import akka.actor.typed.Behavior
import akka.actor.typed.Props
import akka.actor.typed.scaladsl.ActorContext
import akka.cluster.sharding.typed.ClusterShardingSettings
import akka.cluster.sharding.typed.ShardingEnvelope
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.cluster.sharding.typed.scaladsl.EntityRef
import akka.cluster.typed.ClusterSingleton
import akka.cluster.typed.ClusterSingletonSettings
import akka.persistence.typed.scaladsl.PersistentBehaviors
import akka.persistence.typed.scaladsl.PersistentBehaviors.CommandHandler
import akka.persistence.typed.scaladsl.PersistentBehaviors.Effect
import com.bimschas.pwascoring.domain.Contest
import com.bimschas.pwascoring.domain.Contest.ContestAlreadyPlanned
import com.bimschas.pwascoring.domain.Contest.ContestNotPlanned
import com.bimschas.pwascoring.domain.Contest.HeatIdUnknown
import com.bimschas.pwascoring.domain.ContestEvent
import com.bimschas.pwascoring.domain.ContestPlannedEvent
import com.bimschas.pwascoring.domain.HeatId
import com.bimschas.pwascoring.service.HeatActor.HeatCommand
import com.bimschas.pwascoring.service.HeatActor.PassivateHeat

object ContestActor {

  //////////////////
  // Constructors //
  //////////////////

  def apply(system: ActorSystem[_]): ActorRef[ContestCommand] = {
    ClusterSingleton(system).spawn(
      behavior = ContestActor.behavior,
      singletonName = "ContestActor",
      props = Props.empty,
      settings = ClusterSingletonSettings(system),
      terminationMessage = ContestActor.PassivateContest
    )
  }

  ////////////////////////////
  // Commands and Responses //
  ////////////////////////////

  sealed trait ContestCommand

  type PlanContestResponse = Either[ContestAlreadyPlanned.type, ContestPlannedEvent]
  case class PlanContest(heatIds: Set[HeatId], replyTo: ActorRef[PlanContestResponse]) extends ContestCommand

  type GetHeatsResponse = Either[ContestNotPlanned.type, Set[HeatId]]
  case class GetHeats(replyTo: ActorRef[GetHeatsResponse]) extends ContestCommand

  type GetHeatResponse = Either[HeatIdUnknown, EntityRef[HeatCommand]]
  case class GetHeat(heatId: HeatId, replyTo: ActorRef[GetHeatResponse]) extends ContestCommand

  case object PassivateContest extends ContestCommand

  //////////////
  // Behavior //
  //////////////

  val PersistenceId = "ContestPersistenceId"

  val behavior: Behavior[ContestCommand] =
    PersistentBehaviors
      .immutable[ContestCommand, ContestEvent, Contest](
        persistenceId = ContestActor.PersistenceId,
        initialState = Contest.empty,
        commandHandler = contestCommandHandler,
        eventHandler = eventHandler
      )
      .onRecoveryCompleted { (ctx, _) =>
        spawnShardRegion(ctx)
        ()
      }

  ///////////////////
  // Event Handler //
  ///////////////////

  private lazy val eventHandler: (Contest, ContestEvent) => Contest =
    (state, event) => state.handleEvent(event)

  /////////////////////
  // Command Handler //
  /////////////////////

  private lazy val contestCommandHandler: CommandHandler[ContestCommand, ContestEvent, Contest] = {
    case (ctx, state, cmd) =>
      cmd match {

        case PlanContest(heatIds, replyTo) =>
          state.planContest(heatIds) match {
            case Left(contestAlreadyPlanned) => Effect.none.andThen(_ => replyTo ! Left(contestAlreadyPlanned))
            case Right(contestPlannedEvent) =>
              Effect.persist(contestPlannedEvent).andThen { _ =>
                replyTo ! Right(contestPlannedEvent)
              }
          }

        case GetHeats(sender) =>
          Effect.none.andThen { _ =>
            sender ! state.heats
          }

        case GetHeat(heatId, sender) =>
          Effect.none.andThen { _ =>
            val response = state.heats match {
              case Left(ContestNotPlanned)                     => Left(HeatIdUnknown(heatId))
              case Right(heatIds) if !heatIds.contains(heatId) => Left(HeatIdUnknown(heatId))
              case Right(_)                                    => Right(heatEntityRef(ctx, heatId))
            }
            sender ! response
          }

        case PassivateContest =>
          Effect.stop
      }
  }

  private def sharding(ctx: ActorContext[_]) =
    ClusterSharding(ctx.system)

  private def heatEntityRef(ctx: ActorContext[_], heatId: HeatId): EntityRef[HeatCommand] =
    sharding(ctx).entityRefFor(HeatEntityTypeKey, heatId.entityId)

  private def spawnShardRegion(ctx: ActorContext[_]): ActorRef[ShardingEnvelope[HeatCommand]] =
    sharding(ctx).spawn(
      behavior = entityId => HeatActor.heatBehavior(entityId),
      props = Props.empty,
      typeKey = HeatEntityTypeKey,
      settings = ClusterShardingSettings(ctx.system),
      maxNumberOfShards = 100,
      handOffStopMessage = PassivateHeat
    )
}
