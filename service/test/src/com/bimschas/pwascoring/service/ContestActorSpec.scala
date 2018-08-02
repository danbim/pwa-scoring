package com.bimschas.pwascoring.service

import java.util.concurrent.atomic.AtomicInteger

import akka.actor.typed.Props
import akka.cluster.typed.ClusterSingleton
import akka.cluster.typed.ClusterSingletonSettings
import akka.testkit.typed.scaladsl.ActorTestKit
import com.bimschas.pwascoring.domain.HeatId
import com.bimschas.pwascoring.domain.RiderId
import org.scalatest.BeforeAndAfterAll
import org.scalatest.Matchers
import org.scalatest.OptionValues
import org.scalatest.WordSpec
import org.scalatest.concurrent.ScalaFutures

object IdGenerator {
  private val lastId = new AtomicInteger(0)
  def nextId(): Int = lastId.incrementAndGet()
}

//noinspection TypeAnnotation
class ContestActorSpec extends WordSpec
  with ActorTestKit
  with BeforeAndAfterAll
  with ScalaFutures
  with OptionValues
  with Matchers {

  override protected def afterAll(): Unit =
    shutdownTestKit()

  private abstract class ContestScenario {
    protected val singletonManager = ClusterSingleton(system)
    protected val contestActor = singletonManager.spawn(
      behavior = ContestActor.behavior,
      singletonName = "ContestActor",
      props = Props.empty,
      settings = ClusterSingletonSettings(system),
      terminationMessage = ContestActor.PassivateContest
    )
    protected val heatId = {
      val uniquePersistenceId = IdGenerator.nextId()
      HeatId.parse(s"$uniquePersistenceId-a").get
    }

    protected val graham = RiderId(sailNr = "USA-1")
    protected val julian = RiderId(sailNr = "G-901")

    /*protected val contestants = HeatContestants(List(graham, julian))
    protected val probe = TestProbe[Either[ContestAlreadyPlanned, ContestPlanned]]()*/
  }

  // TODO shouldn't we shut down the singleton manager after each test?

  "Contest" when {
/*
    "sent a StartHeat command" must {
      "start the heat if heat is not yet running" in {
        new ContestScenario {
          contestActor ! StartHeat(heatId, contestants, probe.ref)
          probe.expectMessageType[Right[ContestAlreadyPlanned, ContestPlanned]]
        }
      }
      "respond that heat has already started if heat already started" in {
        new ContestScenario {
          contestActor ! StartHeat(heatId, contestants, probe.ref)
          probe.expectMessageType[Right[ContestAlreadyPlanned, ContestPlanned]]

          contestActor ! StartHeat(heatId, contestants, probe.ref)
          probe.expectMessageType[Left[ContestAlreadyPlanned, ContestPlanned]]
        }
      }
    }
    "being restartet" must {
      "remember all scores" in {
        new ContestScenario {
          contestActor ! StartHeat(heatId, contestants, probe.ref)
          val heatActor = probe.expectMessageType[Right[ContestAlreadyPlanned, ContestPlanned]].value.handle

          val grahamsWaveScores = List(
            WaveScore(3.8),
            WaveScore(6.7),
            WaveScore(8.9)
          )
          val juliansWaveScores = List(
            WaveScore(3.8),
            WaveScore(6.7),
            WaveScore(8.9)
          )
          val grahamsJumpScores = List(
            JumpScore(BackLoop, 7.6),
            JumpScore(BackLoop, 5.9)
          )
          val juliansJumpScores = List(
            JumpScore(BackLoop, 7.6),
            JumpScore(BackLoop, 5.9)
          )
          val scores = grahamsWaveScores ++ juliansWaveScores ++ grahamsJumpScores ++ juliansJumpScores

          val waveScoreProbe = TestProbe[Either[RiderIdUnknown, WaveScored]]
          val jumpScoreProbe = TestProbe[Either[RiderIdUnknown, JumpScored]]

          def sendCommandAndExpectConfirmation(riderId: RiderId, score: Score) = {
            score match {
              case score: WaveScore =>
                heatActor ! ScoreWave(riderId, score, waveScoreProbe.ref)
                waveScoreProbe.expectMessage(Right(WaveScored(riderId, score)))
              case score: JumpScore =>
                heatActor ! ScoreJump(riderId, score, jumpScoreProbe.ref)
                jumpScoreProbe.expectMessage(Right(JumpScored(riderId, score)))
            }
          }

          (grahamsWaveScores ++ grahamsJumpScores).foreach(score => sendCommandAndExpectConfirmation(graham, score))
          (juliansWaveScores ++ juliansJumpScores).foreach(score => sendCommandAndExpectConfirmation(julian, score))

          def compareScoreSheets(scoreSheets: Map[RiderId, ScoreSheet]): Unit = {

            scoreSheets.get(graham).value.waveScores should contain allElementsOf grahamsWaveScores
            scoreSheets.get(graham).value.jumpScores should contain allElementsOf grahamsJumpScores

            scoreSheets.get(julian).value.waveScores should contain allElementsOf juliansWaveScores
            scoreSheets.get(julian).value.jumpScores should contain allElementsOf juliansJumpScores
          }

          compareScoreSheets((heatActor ? GetScoreSheets).futureValue)

          heatActor ! PassivateHeat

          val heatActorAfterRestart = (contestActor ? {
            ref: ActorRef[Either[HeatIdUnknown, EntityRef[HeatCommand]]] => GetHeat(heatId, ref)
          }).futureValue.toOption.value
          compareScoreSheets((heatActorAfterRestart ? GetScoreSheets).futureValue)
        }
      }
    }
*/
  }
}
