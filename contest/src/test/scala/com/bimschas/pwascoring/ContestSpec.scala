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
import com.bimschas.pwascoring.Heat.WaveScored
import com.bimschas.pwascoring.domain.BackLoop
import com.bimschas.pwascoring.domain.HeatContestants
import com.bimschas.pwascoring.domain.HeatId
import com.bimschas.pwascoring.domain.JumpScore
import com.bimschas.pwascoring.domain.RiderId
import com.bimschas.pwascoring.domain.ScoreSheet
import com.bimschas.pwascoring.domain.Score
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

            val scores: List[Score] = List(
              WaveScore(graham, BigDecimal(3.8)),
              JumpScore(graham, BackLoop, BigDecimal(7.6)),
              WaveScore(julian, BigDecimal(3.8)),
              WaveScore(graham, BigDecimal(6.7)),
              WaveScore(julian, BigDecimal(6.7)),
              JumpScore(julian, BackLoop, BigDecimal(7.6)),
              JumpScore(julian, BackLoop, BigDecimal(5.9)),
              JumpScore(graham, BackLoop, BigDecimal(5.9)),
              WaveScore(julian, BigDecimal(8.9)),
              WaveScore(graham, BigDecimal(8.9))
            )
            val grahamsWaveScores: List[WaveScore] = scores.collect { case s: WaveScore if s.riderId == graham => s }
            val juliansWaveScores: List[WaveScore] = scores.collect { case s: WaveScore if s.riderId == julian => s }
            val grahamsJumpScores: List[JumpScore] = scores.collect { case s: JumpScore if s.riderId == graham => s }
            val juliansJumpScores: List[JumpScore] = scores.collect { case s: JumpScore if s.riderId == julian => s }

            val waveScoreProbe = TestProbe[WaveScored]
            val jumpScoreProbe = TestProbe[JumpScored]

            scores foreach {
              case score: WaveScore =>
                heatActor ! ScoreWave(score, waveScoreProbe.ref)
                waveScoreProbe.expectMessage(WaveScored(score))
              case score: JumpScore =>
                heatActor ! ScoreJump(score, jumpScoreProbe.ref)
                jumpScoreProbe.expectMessage(JumpScored(score))
            }

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
