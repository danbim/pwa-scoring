package com.bimschas.pwascoring.rest

import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.http.scaladsl.unmarshalling.Unmarshal
import com.bimschas.pwascoring.ContestService
import com.bimschas.pwascoring.HeatService
import com.bimschas.pwascoring.domain.Generators
import com.bimschas.pwascoring.domain.HeatContestants
import com.bimschas.pwascoring.domain.HeatId
import com.bimschas.pwascoring.domain.RiderId
import com.bimschas.pwascoring.domain.ScoreSheet
import com.bimschas.pwascoring.rest.json.ContestJsonSupport
import org.scalacheck.Gen
import org.scalatest.Matchers
import org.scalatest.WordSpecLike
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.prop.PropertyChecks

import scala.concurrent.Future

class RestServiceSpec extends WordSpecLike
  with Matchers
  with ScalatestRouteTest
  with Generators
  with PropertyChecks
  with ContestJsonSupport
  with ScalaFutures {

  "WebService" must {
    "return list of heats at /contest" in {
      forAll(Gen.containerOfN[Set, HeatId](5, heatIdGen)) { heatIds: Set[HeatId] =>
        new RunningContest(heatIds) {
          HttpRequest(uri = "/contest") ~> webService.route ~> check {
            status.isSuccess() shouldEqual true
            Unmarshal(response.entity).to[Set[HeatId]].futureValue shouldBe heatIds
          }
        }
      }
    }
  }

  private abstract class RunningContest(heats: Set[HeatId]) {
    private val config = RestServiceConfig("localhost", 8888)
    private val contestService = MockContestService(heats)
    protected val webService: RestService = RestService(config, contestService)
  }

  private case class MockContestService(_heats: Set[HeatId]) extends ContestService {
    override def startHeat(heatId: HeatId, contestants: HeatContestants): Future[HeatService] =
      Future.failed(new NotImplementedError("implement me")) // TODO implement me
    override def heats(): Future[Set[HeatId]] =
      Future.successful(_heats)
    override def heat(heatId: HeatId): Future[HeatService] =
      Future.successful(MockHeatService(heatId))
  }

  private case class MockHeatService(heatId: HeatId) extends HeatService {
    override def contestants: Future[HeatContestants] =
      Future.failed(new NotImplementedError("implement me")) // TODO implement me
    override def scoreSheets: Future[Map[RiderId, ScoreSheet]] =
      Future.failed(new NotImplementedError("implement me")) // TODO implement me
  }
}
