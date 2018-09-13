package com.bimschas.pwascoring.rest

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshalling.sse.EventStreamMarshalling._
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.sse.ServerSentEvent
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.ExceptionHandler
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.DebuggingDirectives
import akka.persistence.query.PersistenceQuery
import akka.persistence.query.journal.leveldb.scaladsl.LeveldbReadJournal
import akka.stream.ActorMaterializer
import com.bimschas.pwascoring.domain.Contest.ContestAlreadyPlanned
import com.bimschas.pwascoring.domain.Contest.ContestNotPlanned
import com.bimschas.pwascoring.domain.Contest.HeatIdUnknown
import com.bimschas.pwascoring.domain.Heat
import com.bimschas.pwascoring.domain.Heat.HeatAlreadyEnded
import com.bimschas.pwascoring.domain.Heat.HeatAlreadyPlanned
import com.bimschas.pwascoring.domain.Heat.HeatAlreadyStarted
import com.bimschas.pwascoring.domain.Heat.HeatNotPlanned
import com.bimschas.pwascoring.domain.Heat.HeatNotStarted
import com.bimschas.pwascoring.domain.Heat.RiderIdUnknown
import com.bimschas.pwascoring.domain.HeatEvent
import com.bimschas.pwascoring.domain.HeatId
import com.bimschas.pwascoring.domain.JumpScore
import com.bimschas.pwascoring.domain.RiderId
import com.bimschas.pwascoring.domain.WaveScore
import com.bimschas.pwascoring.rest.RestService.log
import com.bimschas.pwascoring.service.ContestService
import com.bimschas.pwascoring.service.HeatIdOps
import com.bimschas.pwascoring.service.HeatService
import com.bimschas.pwascoring.service.HeatService.HeatServiceError
import scalaz.zio.ExitResult
import scalaz.zio.IO
import scalaz.zio.RTS

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.duration.DurationLong
import scala.util.control.NoStackTrace

case class RestServiceConfig(hostname: String, port: Int)

object RestService {
  private val log: org.slf4j.Logger = org.slf4j.LoggerFactory.getLogger(classOf[RestService])
}

case class RestService(
  config: RestServiceConfig, contestService: ContestService
)(implicit system: ActorSystem, materializer: ActorMaterializer, ec: ExecutionContext) extends ContestJsonSupport with RTS {

  private lazy val bindingFuture: Future[Http.ServerBinding] =
    Http().bindAndHandle(DebuggingDirectives.logRequestResult(("REST", Logging.DebugLevel))(route), config.hostname, config.port)

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
    val getHeat            = get  & path("contest" / "heats" / HeatIdSegment)
    val getHeatEvents      = get  & path("contest" / "heats" / HeatIdSegment / "events")
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
    import Endpoints._

    putHeats {
      entity(as[ContestSpec]) { contestSpec =>
        onSuccess(contestService.planContest(contestSpec.heatIds)) {
          case Left(ContestAlreadyPlanned) => failWith(ContestAlreadyPlannedException)
          case Right(_) => complete(OK)
        }
      }
    } ~
    getHeats {
      onSuccess(contestService.heats()) {
        case Left(ContestNotPlanned) => failWith(ContestNotPlannedException)
        case Right(heatIds) => complete(heatIds)
      }
    } ~
    getHeat { heatId =>
      get {
        complete {
          val persistenceQuery = PersistenceQuery(system).readJournalFor[LeveldbReadJournal](LeveldbReadJournal.Identifier)
          persistenceQuery
            .eventsByPersistenceId(heatId.entityId, 0L, Long.MaxValue)
            .map(_.event)
            .collectType[HeatEvent]
            .map(heatEvent => heatEvent)
            .scan(Heat(heatId))((heat, heatEvent) => heat.handleEvent(heatEvent))
            .map(heat => HeatLiveStreamState.apply(heat))
            .map(heatLiveStreamState => ServerSentEvent(asJson(heatLiveStreamState).toString()))
            .keepAlive(10.seconds, () => ServerSentEvent.heartbeat)
        }
      }
    } ~
    getHeatEvents { heatId =>
      get {
        complete {
          val persistenceQuery = PersistenceQuery(system).readJournalFor[LeveldbReadJournal](LeveldbReadJournal.Identifier)
          persistenceQuery
            .eventsByPersistenceId(heatId.entityId, 0L, Long.MaxValue)
            .filter(_.event.isInstanceOf[HeatEvent])
            .map { envelope =>
              val heatEvent = envelope.event.asInstanceOf[HeatEvent]
              ServerSentEvent(
                data = asJson(heatEvent).toString(),
                `type` = heatEvent.getClass.getSimpleName,
                id = envelope.sequenceNr.toString
              )
            }
            .keepAlive(10.seconds, () => ServerSentEvent.heartbeat)
        }
      }
    } ~
    putHeat { heatId =>
      parameter(('startHeat.?, 'endHeat.?)) { (startHeat, endHeat) =>
        if (startHeat.isDefined && startHeat.contains("true")) {
          withExistingHeat(heatId)(_.startHeat()) {
            case HeatNotPlanned => failWith(HeatNotPlannedException(heatId))
            case HeatAlreadyStarted => failWith(HeatAlreadyStartedException(heatId))
            case HeatAlreadyEnded => failWith(HeatAlreadyEndedException(heatId))
          }{ _ =>
            complete(OK)
          }
        }
        else if (endHeat.isDefined && endHeat.contains("true")) {
          withExistingHeat(heatId)(_.endHeat()) {
            case HeatNotStarted => failWith(HeatNotStartedException(heatId))
            case HeatAlreadyEnded => failWith(HeatAlreadyEndedException(heatId))
          }{ _ =>
            complete(OK)
          }
        }
        else {
          entity(as[HeatSpec]) { heatSpec =>
            withExistingHeat(heatId)(_.planHeat(heatSpec.contestants, heatSpec.rules)) {
              case HeatAlreadyPlanned => failWith(HeatAlreadyPlannedException(heatId))
            } { _ =>
              complete(OK)
            }
          }
        }
      }
    } ~
    getHeatContestants { heatId =>
      withExistingHeat(heatId)(_.contestants()) {
        case HeatNotPlanned => failWith(HeatNotPlannedException(heatId))
      } { contestants =>
        complete(contestants)
      }
    } ~
    getHeatScoreSheets { heatId =>
      withExistingHeat(heatId)(_.scoreSheets()) {
        case HeatNotPlanned => failWith(HeatNotPlannedException(heatId))
      } { scoreSheets =>
        complete(scoreSheets)
      }
    } ~
    postHeatWaveScore { case (heatId, riderId) =>
      entity(as[WaveScore]) { waveScore =>
        withExistingHeat(heatId)(_.score(riderId, waveScore)) {
          case HeatNotStarted => failWith(HeatNotStartedException(heatId))
          case HeatAlreadyEnded => failWith(HeatAlreadyEndedException(heatId))
          case RiderIdUnknown(id) => failWith(RiderIdUnknownException(id))
        } { _ =>
          complete(OK)
        }
      }
    } ~
    postHeatJumpScore { case (heatId, riderId) =>
      entity(as[JumpScore]) { jumpScore =>
        withExistingHeat(heatId)(_.score(riderId, jumpScore)) {
          case HeatNotStarted => failWith(HeatNotStartedException(heatId))
          case HeatAlreadyEnded => failWith(HeatAlreadyEndedException(heatId))
          case RiderIdUnknown(id) => failWith(RiderIdUnknownException(id))
        } { _ =>
          complete(OK)
        }
      }
    }
  }

  private def withExistingHeat[E, V](heatId: HeatId)
    (onHeatService: HeatService => IO[Either[HeatServiceError, E], V])
    (errToRoute: E => Route)
    (valueToRoute: V => Route): Route = {
    onSuccess(contestService.heat(heatId)) {
      case Left(HeatIdUnknown(id)) => failWith(HeatIdUnknownException(id))
      case Right(heatService) =>
        unsafeRunSync(onHeatService(heatService).attempt) match {

          case ExitResult.Completed(Left(Left(HeatServiceError(cause)))) =>
            log.error("Call to heat service failed: {}", cause)
            complete(HttpResponse(StatusCodes.InternalServerError, entity = "Heat Service currently not available"))

          case ExitResult.Completed(Left(Right(err))) =>
            errToRoute(err)

          case ExitResult.Completed(Right(resp)) =>
            valueToRoute(resp)

          case ExitResult.Failed(_, defects) =>
            log.error("Call to heat service failed with defects: {}", defects)
            complete(HttpResponse(StatusCodes.InternalServerError, entity = "Internal server error"))

          case ExitResult.Terminated(causes) =>
            log.debug("Call to heat service terminated with defects: {}", causes)
            complete(HttpResponse(StatusCodes.InternalServerError, entity = "Internal server error"))
        }
    }
  }

  def startup(): Future[Unit] = {
    bindingFuture.map(_ => println(s"Started web service on http://${config.hostname}:${config.port}"))
  }

  def shutdown(): Future[Unit] = {
    println(s"Shutting down web service on http://${config.hostname}:${config.port}")
    for {
      binding <- bindingFuture
      _ <- binding.unbind()
      _ <- system.terminate()
    } yield ()
  }
}
