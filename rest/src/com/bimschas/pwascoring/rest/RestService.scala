package com.bimschas.pwascoring.rest

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.ExceptionHandler
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.DebuggingDirectives
import akka.stream.ActorMaterializer
import com.bimschas.pwascoring.ContestActor.HeatStarted
import com.bimschas.pwascoring.ContestService
import com.bimschas.pwascoring.domain.Contest.HeatAlreadyStarted
import com.bimschas.pwascoring.domain.Contest.HeatIdUnknown
import com.bimschas.pwascoring.domain.Heat.RiderIdUnknown
import com.bimschas.pwascoring.domain.HeatContestants
import com.bimschas.pwascoring.domain.HeatId
import com.bimschas.pwascoring.domain.JumpScore
import com.bimschas.pwascoring.domain.RiderId
import com.bimschas.pwascoring.domain.WaveScore
import com.bimschas.pwascoring.rest.json.ContestJsonSupport

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.util.control.NoStackTrace

case class RestServiceConfig(hostname: String, port: Int)

case class RestService(
  config: RestServiceConfig, contestService: ContestService
)(implicit system: ActorSystem, materializer: ActorMaterializer, ec: ExecutionContext) extends ContestJsonSupport {

  private lazy val bindingFuture: Future[Http.ServerBinding] =
    Http().bindAndHandle(DebuggingDirectives.logRequestResult("REST", Logging.DebugLevel)(route), config.hostname, config.port)

  //noinspection TypeAnnotation
  private object PathMatchers {
    val HeatIdSegment = Segment.flatMap(s => HeatId.parse(s).toOption)
    val RiderIdSegment = Segment.map(RiderId.apply)
  }

  //noinspection TypeAnnotation
  private object Endpoints {

    import PathMatchers._

    // format: OFF
    val getHeats               = get  & path("contest")
    val startHeat              = post & path("contest" / HeatIdSegment)
    val getContestantsByHeatId = get  & path("contest" / HeatIdSegment / "contestants")
    val getScoreSheets         = get  & path("contest" / HeatIdSegment / "scoreSheets")
    val postWaveScore          = post & path("contest" / HeatIdSegment / "waveScores" / RiderIdSegment)
    val postJumpScore          = post & path("contest" / HeatIdSegment / "jumpScores" / RiderIdSegment)
    // format: ON
  }

  private sealed trait BadRequest
  private case class HeatIdUnknownException(heatId: HeatId) extends IllegalArgumentException with NoStackTrace with BadRequest
  private case class RiderIdUnknownException(riderId: RiderId) extends IllegalArgumentException with NoStackTrace with BadRequest
  private case class HeatAlreadyStartedException(heatId: HeatId) extends IllegalArgumentException with NoStackTrace with BadRequest

  private val exceptionHandler: ExceptionHandler = ExceptionHandler {
    case badRequest: BadRequest => badRequest match {
      case HeatIdUnknownException(heatId) => complete(HttpResponse(StatusCodes.BadRequest, entity = s"Unknown heatId $heatId"))
      case RiderIdUnknownException(riderId) => complete(HttpResponse(StatusCodes.BadRequest, entity = s"Unknown riderId $riderId"))
      case HeatAlreadyStartedException(heatId) => complete(HttpResponse(StatusCodes.BadRequest, entity = s"Heat $heatId already started"))
    }
  }

  lazy val route: Route = handleExceptions(exceptionHandler) {
    import Endpoints._

    getHeats {
      onSuccess(contestService.heats()) { heatIds =>
        complete(heatIds)
      }
    } ~
    startHeat { heatId =>
      entity(as[HeatContestants]) { heatContestants =>
        onSuccess(contestService.startHeat(heatId, heatContestants)) {
          case Left(HeatAlreadyStarted(id)) => failWith(HeatAlreadyStartedException(id))
          case Right(HeatStarted(_)) => complete(OK)
        }
      }
    } ~
    getContestantsByHeatId { heatId =>
      onSuccess(contestService.heat(heatId)) {
        case Left(HeatIdUnknown(id)) => failWith(HeatIdUnknownException(id))
        case Right(heatService) => onSuccess(heatService.contestants)(complete(_))
      }
    } ~
    getScoreSheets { heatId =>
      onSuccess(contestService.heat(heatId)) {
        case Left(HeatIdUnknown(id)) => failWith(HeatIdUnknownException(id))
        case Right(heatService) => onSuccess(heatService.scoreSheets)(scoreSheets => complete(scoreSheets))
      }
    } ~
    postWaveScore { case (heatId, riderId) =>
      entity(as[WaveScore]) { waveScore =>
        onSuccess(contestService.heat(heatId)) {
          case Left(HeatIdUnknown(id)) => failWith(HeatIdUnknownException(id))
          case Right(heatService) =>
            onSuccess(heatService.score(riderId, waveScore)) {
              case Left(RiderIdUnknown(id)) => failWith(RiderIdUnknownException(id))
              case Right(waveScored) => complete(waveScored)
            }
        }
      }
    } ~
    postJumpScore { case (heatId, riderId) =>
      entity(as[JumpScore]) { jumpScore =>
        onSuccess(contestService.heat(heatId)) {
          case Left(HeatIdUnknown(id)) => failWith(HeatIdUnknownException(id))
          case Right(heatService) =>
            onSuccess(heatService.score(riderId, jumpScore)) {
              case Left(RiderIdUnknown(id)) => failWith(RiderIdUnknownException(id))
              case Right(jumpScored) => complete(jumpScored)
            }
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
