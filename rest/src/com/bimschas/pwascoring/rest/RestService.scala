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
import com.bimschas.pwascoring.ContestService
import com.bimschas.pwascoring.HeatService
import com.bimschas.pwascoring.domain.Contest.ContestAlreadyPlanned
import com.bimschas.pwascoring.domain.Contest.ContestNotPlanned
import com.bimschas.pwascoring.domain.Contest.HeatIdUnknown
import com.bimschas.pwascoring.domain.Heat.HeatAlreadyEnded
import com.bimschas.pwascoring.domain.Heat.HeatAlreadyPlanned
import com.bimschas.pwascoring.domain.Heat.HeatAlreadyStarted
import com.bimschas.pwascoring.domain.Heat.HeatNotPlanned
import com.bimschas.pwascoring.domain.Heat.HeatNotStarted
import com.bimschas.pwascoring.domain.Heat.RiderIdUnknown
import com.bimschas.pwascoring.domain.HeatContestants
import com.bimschas.pwascoring.domain.HeatId
import com.bimschas.pwascoring.domain.JumpScore
import com.bimschas.pwascoring.domain.RiderId
import com.bimschas.pwascoring.domain.WaveScore

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
    val putHeats           = put  & path("contest" / "heats")
    val getHeats           = get  & path("contest" / "heats")
    val putHeat            = put  & path("contest" / "heats" / HeatIdSegment)
    val getHeatContestants = get  & path("contest" / "heats" / HeatIdSegment / "contestants")
    val getHeatScoreSheets = get  & path("contest" / "heats" / HeatIdSegment / "scoreSheets")
    val postHeatWaveScore  = post & path("contest" / "heats" / HeatIdSegment / "waveScores" / RiderIdSegment)
    val postHeatJumpScore  = post & path("contest" / "heats" / HeatIdSegment / "jumpScores" / RiderIdSegment)
    // format: ON
  }

  private sealed trait BadRequest
  private case object ContestAlreadyPlannedException extends IllegalStateException(s"Contest already planned") with NoStackTrace with BadRequest
  private case object ContestNotPlannedException extends IllegalStateException(s"Contest was not yet planned") with NoStackTrace with BadRequest
  private case class HeatNotPlannedException(heatId: HeatId) extends IllegalStateException(s"Heat $heatId was not yet planned") with NoStackTrace with BadRequest
  private case class HeatAlreadyPlannedException(heatId: HeatId) extends IllegalStateException(s"Heat $heatId is already planned") with NoStackTrace with BadRequest
  private case class HeatNotStartedException(heatId: HeatId) extends IllegalStateException(s"Heat $heatId has not yet started") with NoStackTrace with BadRequest
  private case class HeatAlreadyStartedException(heatId: HeatId) extends IllegalStateException(s"Heat $heatId already started") with NoStackTrace with BadRequest
  private case class HeatIdUnknownException(heatId: HeatId) extends IllegalArgumentException(s"Unknown heatId $heatId") with NoStackTrace with BadRequest
  private case class RiderIdUnknownException(riderId: RiderId) extends IllegalArgumentException(s"Unknown riderId $riderId") with NoStackTrace with BadRequest
  private case class HeatAlreadyEndedException(heatId: HeatId) extends IllegalStateException(s"Heat $heatId already ended") with NoStackTrace with BadRequest

  private val exceptionHandler: ExceptionHandler = ExceptionHandler {
    case badRequest: BadRequest => badRequest match {
      case e: ContestNotPlannedException.type => complete(HttpResponse(StatusCodes.BadRequest, entity = e.getMessage))
      case e: ContestAlreadyPlannedException.type => complete(HttpResponse(StatusCodes.BadRequest, entity = e.getMessage))
      case e: HeatIdUnknownException => complete(HttpResponse(StatusCodes.NotFound, entity = e.getMessage))
      case e: RiderIdUnknownException => complete(HttpResponse(StatusCodes.NotFound, entity = e.getMessage))
      case e: HeatNotPlannedException => complete(HttpResponse(StatusCodes.BadRequest, entity = e.getMessage))
      case e: HeatNotStartedException => complete(HttpResponse(StatusCodes.BadRequest, entity = e.getMessage))
      case e: HeatAlreadyStartedException => complete(HttpResponse(StatusCodes.BadRequest, entity = e.getMessage))
      case e: HeatAlreadyPlannedException => complete(HttpResponse(StatusCodes.BadRequest, entity = e.getMessage))
      case e: HeatAlreadyEndedException => complete(HttpResponse(StatusCodes.BadRequest, entity = e.getMessage))
    }
  }

  lazy val route: Route = handleExceptions(exceptionHandler) {

    Endpoints.putHeats {
      entity(as[ContestSpec]) { contestSpec =>
        onSuccess(contestService.planContest(contestSpec.heatIds)) {
          case Left(ContestAlreadyPlanned) => failWith(ContestAlreadyPlannedException)
          case Right(_) => complete(OK)
        }
      }
    } ~
    Endpoints.getHeats {
      onSuccess(contestService.heats()) {
        case Left(ContestNotPlanned) => failWith(ContestNotPlannedException)
        case Right(heatIds) => complete(heatIds)
      }
    } ~
    Endpoints.putHeat { heatId =>
      parameter('startHeat.?, 'endHeat.?) { (startHeat, endHeat) =>
        if (startHeat.isDefined && startHeat.contains("true")) {
          withExistingHeat(heatId)(_.startHeat()) {
            case Left(HeatNotPlanned) => failWith(HeatNotPlannedException(heatId))
            case Left(HeatAlreadyStarted) => failWith(HeatAlreadyStartedException(heatId))
            case Left(HeatAlreadyEnded) => failWith(HeatAlreadyEndedException(heatId))
            case Right(_) => complete(OK)
          }
        }
        else if (endHeat.isDefined && endHeat.contains("true")) {
          withExistingHeat(heatId)(_.endHeat()) {
            case Left(HeatNotStarted) => failWith(HeatNotStartedException(heatId))
            case Left(HeatAlreadyEnded) => failWith(HeatAlreadyEndedException(heatId))
            case Right(_) => complete(OK)
          }
        }
        else {
          entity(as[HeatContestants]) { contestants =>
            withExistingHeat(heatId)(_.planHeat(contestants)) {
              case Left(HeatAlreadyPlanned) => failWith(HeatAlreadyPlannedException(heatId))
              case Right(_) => complete(OK)
            }
          }
        }
      }
    } ~
    Endpoints.getHeatContestants { heatId =>
      withExistingHeat(heatId)(_.contestants()) {
        case Left(HeatNotPlanned) => failWith(HeatNotPlannedException(heatId))
        case Right(contestants) => complete(contestants)
      }
    } ~
    Endpoints.getHeatScoreSheets { heatId =>
      withExistingHeat(heatId)(_.scoreSheets()) {
        case Left(HeatNotPlanned) => failWith(HeatNotPlannedException(heatId))
        case Right(scoreSheets) => complete(scoreSheets)
      }
    } ~
    Endpoints.postHeatWaveScore { case (heatId, riderId) =>
      entity(as[WaveScore]) { waveScore =>
        withExistingHeat(heatId)(_.score(riderId, waveScore)) {
          case Left(HeatNotStarted) => failWith(HeatNotStartedException(heatId))
          case Left(HeatAlreadyEnded) => failWith(HeatAlreadyEndedException(heatId))
          case Left(RiderIdUnknown(id)) => failWith(RiderIdUnknownException(id))
          case Right(_) => complete(OK)
        }
      }
    } ~
    Endpoints.postHeatJumpScore { case (heatId, riderId) =>
      entity(as[JumpScore]) { jumpScore =>
        withExistingHeat(heatId)(_.score(riderId, jumpScore)) {
          case Left(HeatNotStarted) => failWith(HeatNotStartedException(heatId))
          case Left(HeatAlreadyEnded) => failWith(HeatAlreadyEndedException(heatId))
          case Left(RiderIdUnknown(id)) => failWith(RiderIdUnknownException(id))
          case Right(_) => complete(OK)
        }
      }
    }
  }

  private def withExistingHeat[T, R](heatId: HeatId)(onHeatService: HeatService => Future[R])(toRoute: R => Route): Route = {
    onSuccess(contestService.heat(heatId)) {
      case Left(HeatIdUnknown(id)) => failWith(HeatIdUnknownException(id))
      case Right(heatService) =>
        onSuccess(onHeatService(heatService))(resp => toRoute(resp))
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
