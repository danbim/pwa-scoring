package com.bimschas.pwascoring

import java.util.concurrent.atomic.AtomicInteger

import akka.actor.typed.ActorRef
import akka.actor.typed.Props
import akka.actor.typed.scaladsl.AskPattern._
import akka.cluster.sharding.typed.scaladsl.EntityRef
import akka.cluster.typed.ClusterSingleton
import akka.cluster.typed.ClusterSingletonSettings
import akka.testkit.typed.scaladsl.ActorTestKit
import akka.testkit.typed.scaladsl.TestProbe
import org.scalatest.BeforeAndAfterAll
import org.scalatest.BeforeAndAfterEach
import org.scalatest.Matchers
import org.scalatest.OptionValues
import org.scalatest.WordSpec
import org.scalatest.concurrent.ScalaFutures
import com.bimschas.pwascoring.Contest.GetHeat
import com.bimschas.pwascoring.Contest.HeatAlreadyStarted
import com.bimschas.pwascoring.Contest.HeatIdUnknown
import com.bimschas.pwascoring.Contest.HeatStarted
import com.bimschas.pwascoring.Contest.StartHeat
import com.bimschas.pwascoring.Heat.GetScoreSheets
import com.bimschas.pwascoring.Heat.HeatCommand
import com.bimschas.pwascoring.Heat.JumpScored
import com.bimschas.pwascoring.Heat.PassivateHeat
import com.bimschas.pwascoring.Heat.ScoreJump
import com.bimschas.pwascoring.Heat.ScoreWave
import com.bimschas.pwascoring.Heat.UnknownRiderId
import com.bimschas.pwascoring.Heat.WaveScored
import com.bimschas.pwascoring.domain.BackLoop
import com.bimschas.pwascoring.domain.HeatContestants
import com.bimschas.pwascoring.domain.HeatId
import com.bimschas.pwascoring.domain.JumpScore
import com.bimschas.pwascoring.domain.RiderId
import com.bimschas.pwascoring.domain.ScoreSheet
import com.bimschas.pwascoring.domain.Score
import com.bimschas.pwascoring.domain.WaveScore
import com.bimschas.pwascoring.domain.WaveScore
import com.bimschas.pwascoring.domain.WaveScore

object IdGenerator {
  private val lastId = new AtomicInteger(0)
  def nextId(): Int = lastId.incrementAndGet()
}

//noinspection TypeAnnotation
class ContestSpec extends WordSpec
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
      behavior = Contest.behavior,
      "Contest",
      Props.empty,
      ClusterSingletonSettings(system),
      Contest.PassivateContest
    )
    protected val heatId = {
      val uniquePersistenceId = IdGenerator.nextId()
      HeatId(uniquePersistenceId, Some('a'))
    }

    protected val graham = RiderId(sailNr = "USA-1")
    protected val julian = RiderId(sailNr = "G-901")

    protected val contestants = HeatContestants(graham, julian)
    protected val probe = TestProbe[Either[HeatAlreadyStarted, HeatStarted]]()
  }

  // TODO shouldn't we shut down the singleton manager after each test?

  "Contest" when {
    "sent a StartHeat command" must {
      "start the heat if heat is not yet running" in {
        new ContestScenario {
          contestActor ! StartHeat(heatId, contestants, probe.ref)
          probe.expectMessageType[Right[HeatAlreadyStarted, HeatStarted]]
        }
      }
      "respond that heat has already started if heat already started" in {
        new ContestScenario {
          contestActor ! StartHeat(heatId, contestants, probe.ref)
          probe.expectMessageType[Right[HeatAlreadyStarted, HeatStarted]]

          contestActor ! StartHeat(heatId, contestants, probe.ref)
          probe.expectMessageType[Left[HeatAlreadyStarted, HeatStarted]]
        }
      }
      "scoring multiple waves" must {
        "only score the n highest wave scores" in {
          new ContestScenario {
            contestActor ! StartHeat(heatId, contestants, probe.ref)
            val heatActor = probe.expectMessageType[Right[HeatAlreadyStarted, HeatStarted]].value.handle

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

            val waveScoreProbe = TestProbe[Either[UnknownRiderId, WaveScored]]
            val jumpScoreProbe = TestProbe[Either[UnknownRiderId, JumpScored]]

            def sendAndExpectAnswer(riderId: RiderId, score: Score) = {
              score match {
                case score: WaveScore =>
                  heatActor ! ScoreWave(riderId, score, waveScoreProbe.ref)
                  waveScoreProbe.expectMessage(Right(WaveScored(riderId, score)))
                case score: JumpScore =>
                  heatActor ! ScoreJump(riderId, score, jumpScoreProbe.ref)
                  jumpScoreProbe.expectMessage(Right(JumpScored(riderId, score)))
              }
            }

            (grahamsWaveScores ++ grahamsJumpScores).foreach(score => sendAndExpectAnswer(graham, score))
            (juliansWaveScores ++ juliansJumpScores).foreach(score => sendAndExpectAnswer(julian, score))

            def compareScoreSheets(scoreSheets: Map[RiderId, ScoreSheet]) = {
              scoreSheets.get(graham).value.waveScores should contain allElementsOf grahamsWaveScores
              scoreSheets.get(graham).value.jumpScores should contain allElementsOf grahamsJumpScores
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
    }
  }
}
