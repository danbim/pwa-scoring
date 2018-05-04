package com.bimschas.pwascoring

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.Props
import akka.actor.typed.scaladsl.ActorContext

import akka.cluster.sharding.typed.ClusterShardingSettings
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.cluster.sharding.typed.scaladsl.EntityRef

import akka.persistence.typed.scaladsl.PersistentBehaviors
import akka.persistence.typed.scaladsl.PersistentBehaviors.CommandHandler
import akka.persistence.typed.scaladsl.PersistentBehaviors.Effect

import com.bimschas.pwascoring.HeatActor.HeatCommand
import com.bimschas.pwascoring.HeatActor.PassivateHeat
import com.bimschas.pwascoring.domain.HeatContestants
import com.bimschas.pwascoring.domain.HeatId
import com.bimschas.pwascoring.domain.Implicits.HeatIdOps

object ContestActor {

  ////// contest commands and responses
  sealed trait ContestCommand
  final case class StartHeat(
    heatId: HeatId, contestants: HeatContestants, replyTo: ActorRef[Either[HeatAlreadyStarted, HeatStarted]]
  ) extends ContestCommand
  final case class GetHeat(heatId: HeatId, replyTo: ActorRef[Either[HeatIdUnknown, EntityRef[HeatCommand]]]) extends ContestCommand
  final case object PassivateContest extends ContestCommand

  final case class HeatStarted(handle: EntityRef[HeatCommand])
  final case class HeatAlreadyStarted(handle: EntityRef[HeatCommand])
  final case class HeatIdUnknown(heatId: HeatId)

  ////// contest events
  sealed trait ContestEvent
  final case class HeatStartedEvent(heatId: HeatId) extends ContestEvent

  ////// contest state
  final case class ContestState(heats: Set[HeatId])
  object ContestState {
    def empty = ContestState(Set.empty)
  }

  ////// contest config
  val PersistenceId = "ContestPersistenceId"

  ////// contest handlers and behavior
  private val contestEventHandler: (ContestState, ContestEvent) => ContestState = {
    case (state, event) =>
      event match {
        case HeatStartedEvent(heatId) =>
          println(s"contestEventHandler(HeatStartedEvent($heatId))")
          state.copy(heats = state.heats + heatId)
      }
  }

  private def sharding(ctx: ActorContext[_]) =
    ClusterSharding(ctx.system)

  private def heatEntityRef(ctx: ActorContext[_], heatId: HeatId) =
    sharding(ctx).entityRefFor(heatId.entityTypeKey, heatId.entityId)

  private def spawnHeatEntity(ctx: ActorContext[_], heatId: HeatId, contestants: HeatContestants) =
    sharding(ctx).spawn(
      behavior = entityId => HeatActor.heatBehavior(entityId, heatId, contestants),
      props = Props.empty,
      typeKey = heatId.entityTypeKey,
      settings = ClusterShardingSettings(ctx.system),
      maxNumberOfShards = 100,
      handOffStopMessage = PassivateHeat
    )

  private val contestCommandHandler: CommandHandler[ContestCommand, ContestEvent, ContestState] = {
    case (ctx, state, cmd) =>
      cmd match {

        case StartHeat(heatId, contestants, sender) =>
          println(s"===> StartHeat(headId=$heatId, contestants=$contestants, sender=$sender")
          if (state.heats.contains(heatId)) {
            println(s"===> HeatAlreadyStarted")
            sender ! Left(HeatAlreadyStarted(heatEntityRef(ctx, heatId)))
            Effect.none
          } else {
            println(s"===> HeatStartedEvent")
            Effect.persist(HeatStartedEvent(heatId)).andThen { _ =>
              spawnHeatEntity(ctx, heatId, contestants)
              sender ! Right(HeatStarted(heatEntityRef(ctx, heatId)))
            }
          }

        case GetHeat(heatId, sender) =>
          println(s"===> GetHeat(headId=$heatId, sender=$sender")
          val response =
            if (state.heats.contains(heatId)) Right(heatEntityRef(ctx, heatId))
            else Left(HeatIdUnknown(heatId))
          sender ! response
          Effect.none

        case PassivateContest =>
          Effect.stop
      }
  }

  val behavior: Behavior[ContestCommand] =
    PersistentBehaviors.immutable[ContestCommand, ContestEvent, ContestState](
      persistenceId = ContestActor.PersistenceId,
      initialState = ContestState.empty,
      commandHandler = contestCommandHandler,
      eventHandler = contestEventHandler
    )
}
