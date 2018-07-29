package com.bimschas.pwascoring.rest

import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model.HttpMethods
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.model.RequestEntity
import akka.http.scaladsl.model.StatusCodes.OK
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.testkit.TestKitBase
import com.bimschas.pwascoring.ActorBasedContestService
import com.bimschas.pwascoring.domain.Generators
import com.bimschas.pwascoring.domain.HeatId
import com.bimschas.pwascoring.rest.json.ContestJsonSupport
import org.scalacheck.Gen
import org.scalatest.Matchers
import org.scalatest.WordSpecLike
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.prop.PropertyChecks

import scala.concurrent.Future

class RestServiceIntegrationSpec extends WordSpecLike
  with Matchers
  with ScalatestRouteTest
  with Generators
  with PropertyChecks
  with ContestJsonSupport
  with ScalaFutures
  with TestKitBase {

  private abstract class RunningRestService(heats: Set[HeatId]) {
    private val config = RestServiceConfig("localhost", 8888)
    private val contestService = ActorBasedContestService(system)(system.scheduler, executor)
    protected val restService: RestService = RestService(config, contestService)

    protected def putHeatIds(heatIds: Set[HeatId]): RouteTestResult = {
      Marshal(heatIds).to[RequestEntity].map { requestEntity =>
        HttpRequest(
          uri = "/contest/heats",
          method = HttpMethods.PUT,
          entity = requestEntity
        ) ~> restService.route
      }.futureValue
    }
  }

  "RestService" when {
    "PUT /contest/heats is called" must {
      "plan a contest" in {
        forAll(Gen.containerOfN[Set, HeatId](5, heatIdGen)) { heatIds: Set[HeatId] =>
          new RunningRestService(heatIds) {
            putHeatIds(heatIds) ~> check {
              handled shouldBe true
              status shouldBe OK
              Unmarshal(response.entity).to[Set[HeatId]].futureValue shouldBe heatIds
            }
          }
        }
        pending
      }
      "fail planning a contest if already planned" in {
        pending
      }
    }
    "GET /contest/heats is called" must {
      "not return any heats if contest is not planned yet" in {
        pending
      }
      "return planned heats if contest is planned" in {
        pending
      }
    }
    "PUT /contest/heats/$heatId/ is called" must {
      "plan a heat if not yet planned" in {
        pending
      }
      "fail planning a heat if already planned" in {
        pending
      }
    }
    "PUT /contest/heats/$heatId/ is called with request param started=true" must {
      "start a heat if not yet started" in {
        pending
      }
      "fail starting a heat if already started" in {
        pending
      }
    }
    "PUT /contest/heats/$heatId/ is called with request param ended=true" must {
      "end the heat if running" in {
        pending
      }
      "fail ending the heat if not yet planned" in {
        pending
      }
      "fail ending the heat if not yet running" in {
        pending
      }
      "fail ending the heat if already ended" in {
        pending
      }
    }
    "GET /contest/heats/$heatId/contestants is called" must {
      "return contestants if heat is planned" in {
        pending
      }
      "return contestants if heat is running" in {
        pending
      }
      "return contestants if heat has ended" in {
        pending
      }
      "fail if heat was not yet planned" in {
        pending
      }
    }
    "GET /contest/heats/$heatId/scoreSheets is called" must {
      "return score sheets if heat is planned" in {
        pending
      }
      "return score sheets if heat is running" in {
        pending
      }
      "return score sheets if heat has ended" in {
        pending
      }
      "fail if heat was not yet planned" in {
        pending
      }
    }
  }
}
