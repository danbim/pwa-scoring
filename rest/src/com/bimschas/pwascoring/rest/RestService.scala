package com.bimschas.pwascoring.rest

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import com.bimschas.pwascoring.ContestActor.HeatStarted
import com.bimschas.pwascoring.ContestService
import com.bimschas.pwascoring.HeatActor.JumpScored
import com.bimschas.pwascoring.HeatActor.WaveScored
import com.bimschas.pwascoring.domain.Contest.HeatAlreadyStarted
import com.bimschas.pwascoring.domain.Heat.UnknownRiderId
import com.bimschas.pwascoring.domain.HeatContestants
import com.bimschas.pwascoring.domain.HeatId
import com.bimschas.pwascoring.domain.JumpScore
import com.bimschas.pwascoring.domain.RiderId
import com.bimschas.pwascoring.domain.WaveScore
import com.bimschas.pwascoring.rest.json.ContestJsonSupport

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

case class RestServiceConfig(hostname: String, port: Int)

case class RestService(
  config: RestServiceConfig, contestService: ContestService
)(implicit system: ActorSystem, materializer: ActorMaterializer, ec: ExecutionContext) extends ContestJsonSupport {

  private lazy val bindingFuture: Future[Http.ServerBinding] =
    Http().bindAndHandle(route, config.hostname, config.port)

  //noinspection TypeAnnotation
  private object PathMatchers {
    val HeatIdSegment = Segment.flatMap(s => HeatId.parse(s).toOption)
    val RiderIdSegment = Segment.map(RiderId.apply)
  }

  //noinspection TypeAnnotation
  private object Endpoints {
    import PathMatchers._

    val getHeats               = get  & path("contest")
    val startHeat              = post & path("contest" / HeatIdSegment)
    val getContestantsByHeatId = get  & path("contest" / HeatIdSegment / "contestants")
    val getScoreSheets         = get  & path("contest" / HeatIdSegment / "scoreSheets")
    val postWaveScore          = post & path("contest" / HeatIdSegment / "waveScores" / RiderIdSegment)
    val postJumpScore          = post & path("contest" / HeatIdSegment / "jumpScores" / RiderIdSegment)
  }

  lazy val route: Route = {
    import Endpoints._

    getHeats {
      onSuccess(contestService.heats()) { heatIds =>
        complete(heatIds)
      }
    } ~
    startHeat { heatId =>
      entity(as[HeatContestants]) { heatContestants =>
        onSuccess(contestService.startHeat(heatId, heatContestants)) {
          case Left(HeatAlreadyStarted(runningHeatId)) =>
            complete(HttpResponse(BadRequest, entity = s"Heat $runningHeatId already started"))
          case Right(HeatStarted(_)) =>
            complete(OK)
        }
      }
    } ~
    getContestantsByHeatId { heatId =>
      onSuccess(for {
        heatService <- contestService.heat(heatId)
        contestants <- heatService.contestants
      } yield contestants) { contestants =>
        complete(contestants)
      }
    } ~
    getScoreSheets { heatId =>
      onSuccess(for {
        heatService <- contestService.heat(heatId)
        scoreSheets <- heatService.scoreSheets
      } yield scoreSheets) { scoreSheets =>
        complete(scoreSheets)
      }
    } ~
    postWaveScore { case (heatId, riderId) => // TODO use a correlation ID for enabling at-least-once delivery
      entity(as[WaveScore]) { waveScore =>
        onSuccess(for {
          heatService <- contestService.heat(heatId)
          scoredOrNot <- heatService.score(riderId, waveScore)
        } yield scoredOrNot) {
          case Left(UnknownRiderId(unknownRiderId)) =>
            complete(HttpResponse(BadRequest, entity = s"Unknown riderId ('$unknownRiderId')"))
          case Right(WaveScored(_, _)) =>
            complete(OK)
        }
      }
    } ~
    postJumpScore { case (heatId, riderId) => // TODO use a correlation ID for enabling at-least-once delivery
      entity(as[JumpScore]) { jumpScore =>
        onSuccess(for {
          heatService <- contestService.heat(heatId)
          scoredOrNot <- heatService.score(riderId, jumpScore)
        } yield scoredOrNot) {
          case Left(UnknownRiderId(unkonwnRiderId)) =>
            complete(HttpResponse(BadRequest, entity = s"Unknown riderId ('$unkonwnRiderId')"))
          case Right(JumpScored(_, _)) =>
            complete(OK)
        }
      }
    }
  }

  def startup(): Future[Unit] = {
    bindingFuture.map(_ => println(s"Started web service on http://${config.hostname}:${config.port}"))
  }

  def shutdown(): Future[Unit] = {
    println(s"Shutting down web service on http://${config.hostname}:${config.port}")
    bindingFuture
      .flatMap(_.unbind())
      .map(_ => system.terminate())
  }
}
