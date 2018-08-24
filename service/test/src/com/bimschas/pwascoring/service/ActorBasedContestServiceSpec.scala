package com.bimschas.pwascoring.service

import java.util.concurrent.atomic.AtomicInteger

import com.bimschas.pwascoring.domain.Contest.ContestAlreadyPlanned
import com.bimschas.pwascoring.domain.ContestPlannedEvent

import scala.concurrent.duration.DurationInt

object IdGenerator {
  private val lastId = new AtomicInteger(0)
  def nextId(): Int = lastId.incrementAndGet()
}

//noinspection TypeAnnotation
class ActorBasedContestServiceSpec extends SpecBase {

  "ContestService" when {
    implicit val patienceConfig = PatienceConfig(timeout = 10.seconds)

    "planning a contest" must {
      "plan a contest if it was not yet planned" in {
        withResources(TestContestService()) { contestService =>

          val heatIds = sample(nonEmptySmallSetGen(heatIdGen))
          contestService.planContest(heatIds).futureValue shouldBe Right(ContestPlannedEvent(heatIds))
        }
      }
      "refuse to plan contest if it was already planned" in {
        withResources(TestContestService()) { contestService =>

          val heatIds = sample(nonEmptySmallSetGen(heatIdGen))
          contestService.planContest(heatIds).futureValue shouldBe Right(ContestPlannedEvent(heatIds))

          val heatIds2 = sample(nonEmptySmallSetGen(heatIdGen))
          contestService.planContest(heatIds2).futureValue shouldBe Left(ContestAlreadyPlanned)
        }
      }
    }
  }
}
