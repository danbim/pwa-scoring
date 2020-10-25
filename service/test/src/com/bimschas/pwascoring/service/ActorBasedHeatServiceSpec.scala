package com.bimschas.pwascoring.service

import com.bimschas.pwascoring.domain.HeatContestants
import com.bimschas.pwascoring.domain.HeatId
import org.scalatest.Assertion

import scala.concurrent.duration.DurationLong

class ActorBasedHeatServiceSpec extends SpecBase {

  "ActorBasedHeatService" when {
    implicit val patienceConfig: PatienceConfig = PatienceConfig(timeout = 10.seconds)

    "planning a heat" must {
      "plan a heat if it was not yet planned" in {
        withHeat { (heatId, heatService) =>
          val riderIds = sample(nonEmptySmallSetGen(riderIdGen))
          val heatContestants = HeatContestants(riderIds)
          val heatRules = sample(heatRulesGen)

          val heatPlannedEvent = heatService.planHeat(heatContestants, heatRules).futureValue.right.value

          heatPlannedEvent.heatId shouldBe heatId
          heatPlannedEvent.contestants shouldBe heatContestants
          heatPlannedEvent.rules shouldBe heatRules
        }
      }
    }
  }

  private def withHeat(test: (HeatId, HeatService) => Assertion)(implicit patienceConfig: PatienceConfig): Assertion = {
    withResources(TestContestService()) { contestService =>
      val heatIds = sample(nonEmptySmallSetGen(heatIdGen))
      val contestPlannedEvent = contestService.planContest(heatIds).futureValue.right.value
      val heatId = contestPlannedEvent.heatIds.head

      test(heatId, contestService.heat(heatId).futureValue.right.value)
    }
  }
}
