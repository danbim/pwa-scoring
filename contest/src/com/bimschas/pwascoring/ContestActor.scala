package com.bimschas.pwascoring

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.Props
import akka.actor.typed.scaladsl.ActorContext
import akka.cluster.sharding.typed.ClusterShardingSettings
import akka.cluster.sharding.typed.ShardingEnvelope
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.cluster.sharding.typed.scaladsl.EntityRef
import akka.persistence.typed.scaladsl.PersistentBehaviors
import akka.persistence.typed.scaladsl.PersistentBehaviors.CommandHandler
import akka.persistence.typed.scaladsl.PersistentBehaviors.Effect
import com.bimschas.pwascoring.HeatActor.HeatCommand
import com.bimschas.pwascoring.HeatActor.PassivateHeat
import com.bimschas.pwascoring.domain.Contest
import com.bimschas.pwascoring.domain.Contest.HeatAlreadyStarted
import com.bimschas.pwascoring.domain.Contest.HeatIdUnknown
import com.bimschas.pwascoring.domain.ContestEvent
import com.bimschas.pwascoring.domain.HeatContestants
import com.bimschas.pwascoring.domain.HeatId
import com.bimschas.pwascoring.domain.Implicits.HeatIdOps

object ContestActor {

  sealed trait ContestCommand
  final case class StartHeat(
    heatId: HeatId,
    contestants: HeatContestants,
    replyTo: ActorRef[Either[HeatAlreadyStarted, HeatStarted]]
  ) extends ContestCommand
  final case class GetHeat(
    heatId: HeatId,
    replyTo: ActorRef[Either[HeatIdUnknown, EntityRef[HeatCommand]]]
  ) extends ContestCommand
  final case object PassivateContest extends ContestCommand

  sealed trait ContestResponse
  final case class HeatStarted(handle: EntityRef[HeatCommand]) extends ContestResponse

  val PersistenceId = "ContestPersistenceId"

  val behavior: Behavior[ContestCommand] = {
    PersistentBehaviors.immutable[ContestCommand, ContestEvent, Contest](
      persistenceId = ContestActor.PersistenceId,
      initialState = Contest.empty,
      commandHandler = contestCommandHandler,
      eventHandler = Contest.handleEvent
    )
  }

  private lazy val contestCommandHandler: CommandHandler[ContestCommand, ContestEvent, Contest] = {
    case (ctx, state, cmd) =>
      cmd match {

        case StartHeat(heatId, contestants, replyTo) =>
          state.startHeat(heatId, contestants) match {
            case Left(heatAlreadyStarted) =>
              Effect.none.andThen(_ => replyTo ! Left(heatAlreadyStarted))
            case Right(heatStartedEvent) =>
              Effect.persist(heatStartedEvent).andThen { _ =>
                spawnHeatEntity(ctx, heatId, contestants)
                replyTo ! Right(HeatStarted(heatEntityRef(ctx, heatId)))
              }
          }

        case GetHeat(heatId, sender) =>
          val response =
            if (state.heats.contains(heatId)) Right(heatEntityRef(ctx, heatId))
            else Left(HeatIdUnknown(heatId))
          sender ! response
          Effect.none

        case PassivateContest =>
          Effect.stop
      }
  }

  private def sharding(ctx: ActorContext[_]) =
    ClusterSharding(ctx.system)

  private def heatEntityRef(ctx: ActorContext[_], heatId: HeatId): EntityRef[HeatCommand] =
    sharding(ctx).entityRefFor(heatId.entityTypeKey, heatId.entityId)

  private def spawnHeatEntity(
    ctx: ActorContext[_],
    heatId: HeatId,
    contestants: HeatContestants
  ): ActorRef[ShardingEnvelope[HeatCommand]] = {
    sharding(ctx).spawn(
      behavior = entityId => HeatActor.heatBehavior(entityId, heatId, contestants),
      props = Props.empty,
      typeKey = heatId.entityTypeKey,
      settings = ClusterShardingSettings(ctx.system),
      maxNumberOfShards = 100,
      handOffStopMessage = PassivateHeat
    )
  }
}
