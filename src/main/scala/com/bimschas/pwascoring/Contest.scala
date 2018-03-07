package com.bimschas.pwascoring

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.Props
import akka.actor.typed.scaladsl.ActorContext
import akka.cluster.sharding.typed.ClusterShardingSettings
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.cluster.sharding.typed.scaladsl.EntityRef
import akka.cluster.sharding.typed.scaladsl.EntityTypeKey
import akka.persistence.typed.scaladsl.PersistentBehaviors
import akka.persistence.typed.scaladsl.PersistentBehaviors.CommandHandler
import akka.persistence.typed.scaladsl.PersistentBehaviors.Effect
import com.bimschas.pwascoring.Model.HeatContestants
import com.bimschas.pwascoring.Model.HeatId
import com.bimschas.pwascoring.Model.RiderId

object Model {
  final case class RiderId(sailNr: String)
  final case class HeatId(number: Int, classifier: Option[Char])
  final case class HeatContestants(riderA: RiderId, riderB: RiderId)
}

object Contest {

  ////// contest commands and responses
  sealed trait ContestCommand
  final case class StartHeat(
    heatId: HeatId, contestants: HeatContestants, replyTo: ActorRef[Either[HeatAlreadyStarted, HeatStarted]]
  ) extends ContestCommand
  final case class GetHeat(heatId: HeatId, replayTo: ActorRef[Either[HeatIdUnknown, EntityRef[HeatCommand]]]) extends ContestCommand
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
      behavior = entityId => heatBehavior(entityId, heatId, contestants),
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
              val ref = spawnHeatEntity(ctx, heatId, contestants)
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
      persistenceId = Contest.PersistenceId,
      initialState = ContestState.empty,
      commandHandler = contestCommandHandler,
      eventHandler = contestEventHandler
    )

  ////// heat commands
  sealed trait HeatCommand
  final case class ScoreWave(riderId: RiderId, waveScore: WaveScore, replyTo: ActorRef[WaveScored]) extends HeatCommand
  final case class ScoreJump(riderId: RiderId, jumpScore: JumpScore, replyTo: ActorRef[JumpScored]) extends HeatCommand
  final case class GetScoreSheets(replyTo: ActorRef[Map[RiderId, RiderScoreSheet]]) extends HeatCommand
  final case object PassivateHeat extends HeatCommand

  final case class WaveScored(riderId: RiderId, waveScore: WaveScore)
  final case class JumpScored(riderId: RiderId, waveScore: JumpScore)

  ////// heat events
  sealed trait HeatEvent
  final case class WaveScoredEvent(riderId: RiderId, waveScore: WaveScore) extends HeatEvent
  final case class JumpScoredEvent(riderId: RiderId, jumpScore: JumpScore) extends HeatEvent

  ////// heat state
  final case class WaveScore(score: Int)
  sealed trait JumpType
  final case object TableTop extends JumpType
  final case object FrontLoop extends JumpType
  final case object BackLoop extends JumpType
  final case class JumpScore(score: Int, jumpType: JumpType)
  final case class RiderScoreSheet(waveScores: List[WaveScore], jumpScores: List[JumpScore]) {
    def +\(waveScore: WaveScore): RiderScoreSheet = copy(waveScores = waveScores :+ waveScore)
    def +/(jumpScore: JumpScore): RiderScoreSheet = copy(jumpScores = jumpScores :+ jumpScore)
  }
  object RiderScoreSheet {
    def empty: RiderScoreSheet = RiderScoreSheet(List.empty, List.empty)
  }
  final case class HeatState(scoreSheets: Map[RiderId, RiderScoreSheet]) {
    def +\(riderIdAndWaveScore: (RiderId, WaveScore)): HeatState = {
      val (riderId, waveScore) = riderIdAndWaveScore
      copy(scoreSheets = scoreSheets + (riderId -> (scoreSheets.getOrElse(riderId, RiderScoreSheet.empty) +\ waveScore)))
    }
    def +/(riderIdAndJumpScore: (RiderId, JumpScore)): HeatState = {
      val (riderId, jumpScore) = riderIdAndJumpScore
      copy(scoreSheets = scoreSheets + (riderId -> (scoreSheets.getOrElse(riderId, RiderScoreSheet.empty) +/ jumpScore)))
    }
  }
  object HeatState {
    def empty(contestants: HeatContestants): HeatState =
      HeatState(Map(
        contestants.riderA -> RiderScoreSheet.empty,
        contestants.riderB -> RiderScoreSheet.empty
      ))
  }

  ////// heat handlers and behavior
  private val heatEventHandler: (HeatState, HeatEvent) => HeatState = {
    case (state, event) =>
      event match {
        case WaveScoredEvent(riderId, waveScore) => state +\ (riderId -> waveScore)
        case JumpScoredEvent(riderId, jumpScore) => state +/ (riderId -> jumpScore)
      }
  }

  private def heatBehavior(entityId: String, heatId: HeatId, contestants: HeatContestants): Behavior[HeatCommand] =
    PersistentBehaviors.immutable[HeatCommand, HeatEvent, HeatState](
      persistenceId = entityId,
      initialState = HeatState.empty(contestants),
      commandHandler = heatCommandHandler,
      eventHandler = heatEventHandler
    )

  private val heatCommandHandler: CommandHandler[HeatCommand, HeatEvent, HeatState] = {
    case (_, state, cmd) =>
      cmd match {
        case ScoreWave(riderId, waveScore, replyTo) =>
          Effect.persist(WaveScoredEvent(riderId, waveScore)).andThen { _ =>
            replyTo ! WaveScored(riderId, waveScore)
          }
        case ScoreJump(riderId, jumpScore, replyTo) =>
          Effect.persist(JumpScoredEvent(riderId, jumpScore)).andThen { _ =>
            replyTo ! JumpScored(riderId, jumpScore)
          }
        case GetScoreSheets(replyTo) =>
          replyTo ! state.scoreSheets
          Effect.none
        case PassivateHeat =>
          Effect.stop
      }
  }

  implicit class HeatIdOps(val heatId: HeatId) extends AnyVal {
    def actorName: String = s"HeatActor_${heatId.number}_${heatId.classifier}"
    def entityId: String = s"Heat_${heatId.number}_${heatId.classifier}"
    def entityTypeKey: EntityTypeKey[HeatCommand] = EntityTypeKey[HeatCommand]("Heat")
  }
}
