package com.bimschas.pwascoring.service

import akka.actor.typed.ActorRef
import akka.actor.typed.ActorSystem
import akka.cluster.sharding.typed.scaladsl.EntityRef
import akka.testkit.typed.scaladsl.TestProbe
import com.bimschas.pwascoring.domain.Contest.ContestAlreadyPlanned
import com.bimschas.pwascoring.domain.ContestPlannedEvent
import com.bimschas.pwascoring.domain.Heat.PlanHeatError
import com.bimschas.pwascoring.domain.HeatContestants
import com.bimschas.pwascoring.domain.HeatId
import com.bimschas.pwascoring.domain.HeatPlannedEvent
import com.bimschas.pwascoring.service.ContestActor.GetHeat
import com.bimschas.pwascoring.service.ContestActor.GetHeatResponse
import com.bimschas.pwascoring.service.ContestActor.PlanContest
import com.bimschas.pwascoring.service.ContestActor.PlanContestResponse
import com.bimschas.pwascoring.service.HeatActor.HeatCommand
import com.bimschas.pwascoring.service.HeatActor.PlanHeat
import com.bimschas.pwascoring.service.HeatActor.PlanHeatResponse

class ContestActorSpec extends SpecBase {

  private implicit var system: ActorSystem[_] = _

  override protected def beforeAll(): Unit =
    system = TestActorSystem()

  override protected def afterAll(): Unit =
    Option(system).foreach(_.terminate())

  object TestRunningHeatActor {

    def apply(
      contestActor: ActorRef[ContestActor.ContestCommand],
      heatId: HeatId
    )(implicit system: ActorSystem[_]): EntityRef[HeatCommand] = {

      // plan contest
      val planContestProbe = TestProbe[PlanContestResponse]()
      contestActor ! PlanContest(Set(heatId), planContestProbe.ref)
      planContestProbe.expectMessageType[Right[ContestAlreadyPlanned.type, ContestPlannedEvent]]

      // retrieve heat ActorRef
      val getHeatProbe = TestProbe[GetHeatResponse]()
      contestActor ! GetHeat(heatId, getHeatProbe.ref)
      getHeatProbe.expectMessageType[GetHeatResponse].right.value
    }
  }

  "ContestActor" when {
    "sent a PlanHeat command" must {
      "plan the heat" in {

        // GIVEN
        val contestActor = TestContestActor(system)
        val heatId = sample(heatIdGen)
        val heatActor = TestRunningHeatActor(contestActor, heatId)
        val heatRiderIds = sample(nonEmptySmallSetGen(riderIdGen))
        val heatContestants = HeatContestants(heatRiderIds)
        val heatRules = sample(heatRulesGen)

        // WHEN
        val planHeatProbe = TestProbe[PlanHeatResponse]()
        heatActor ! PlanHeat(heatContestants, heatRules, planHeatProbe.ref)

        // THEN
        val heatPlannedEvent = planHeatProbe.expectMessageType[Right[PlanHeatError, HeatPlannedEvent]].value
        heatPlannedEvent.heatId shouldBe heatId
        heatPlannedEvent.contestants shouldBe heatContestants
        heatPlannedEvent.rules shouldBe heatRules
      }
    }
  }
}

/*
    "sent a StartHeat command" must {
      "start the heat if heat is not yet running" in {
        withResources(TestContestActor()) { contestActor =>

          val heatId = sample(heatIdGen)
          val heatActor = TestRunningHeatActor(contestActor, heatId)

          heatActor ! PlanHeat
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
*/
/*
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
