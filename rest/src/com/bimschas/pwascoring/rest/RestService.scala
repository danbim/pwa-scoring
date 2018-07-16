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
import com.bimschas.pwascoring.HeatActor.WaveScored
import com.bimschas.pwascoring.domain.Contest.HeatAlreadyStarted
import com.bimschas.pwascoring.domain.Heat.UnknownRiderId
import com.bimschas.pwascoring.domain.HeatContestants
import com.bimschas.pwascoring.domain.HeatId
import com.bimschas.pwascoring.domain.RiderId
import com.bimschas.pwascoring.domain.WaveScore
import com.bimschas.pwascoring.rest.json.ContestJsonSupport

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.util.Try

case class RestServiceConfig(hostname: String, port: Int)

case class RestService(
  config: RestServiceConfig, contestService: ContestService
)(implicit system: ActorSystem, materializer: ActorMaterializer, ec: ExecutionContext) extends ContestJsonSupport {

  private lazy val bindingFuture: Future[Http.ServerBinding] =
    Http().bindAndHandle(route, config.hostname, config.port)

  lazy val route: Route =
    path("contest") {
      get {
        onSuccess(contestService.heats()) { heatIds =>
          complete(heatIds)
        }
      }
    } ~ path("contest" / Segment) { heatIdString: String =>
      post {
        entity(as[HeatContestants]) { heatContestants =>
          onSuccess(for {
            heatId <- parseHeatId(heatIdString) // TODO use Akka Http features to parse into HeatId directly
            startedOrNot <- contestService.startHeat(heatId, heatContestants)
          } yield startedOrNot) {
            case Left(HeatAlreadyStarted(heatId)) =>
              complete(HttpResponse(BadRequest, entity = s"Heat $heatId already started"))
            case Right(HeatStarted(_)) =>
              complete(OK)
          }
        }
      }
    } ~ path("contest" / Segment / "contestants") { heatIdString: String =>
      get {
        onSuccess(for {
          heatId <- parseHeatId(heatIdString) // TODO use Akka Http features to parse into HeatId directly
          heatService <- contestService.heat(heatId)
          contestants <- heatService.contestants
        } yield contestants) { contestants =>
          complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, contestants.toString))
        }
      }
    } ~ path("contest" / Segment / "scoreSheets") { heatIdString: String =>
      get {
        onSuccess(for {
          heatId <- Future.fromTry(HeatId.parse(heatIdString)) // TODO use Akka Http features to parse into HeatId directly
          heatService <- contestService.heat(heatId)
          scoreSheets <- heatService.scoreSheets
        } yield scoreSheets) { scoreSheets =>
          complete(scoreSheets)
        }
      }
    } ~ path("contest" / Segment / "waveScores" / Segment) { case (heatIdString: String, riderIdString) => // TODO use a correlation ID for enabling at-least-once delivery
      post {
        entity(as[WaveScore]) { waveScore =>
          onSuccess(for {
            heatId <- parseHeatId(heatIdString)
            riderId <- parseRiderId(riderIdString)
            heatService <- contestService.heat(heatId)
            scoredOrNot <- heatService.score(riderId, waveScore)
          } yield scoredOrNot) {
            case Left(UnknownRiderId(riderId)) =>
              complete(HttpResponse(BadRequest, entity = s"Unknown riderId ('$riderId')"))
            case Right(WaveScored(_, _)) =>
              complete(OK)
          }
        }
      }
    }

  private def parseHeatId(heatIdString: String): Future[HeatId] =
    Future.fromTry(HeatId.parse(heatIdString)) // TODO use Akka Http features to parse into HeatId directly

  private def parseRiderId(riderIdString: String): Future[RiderId] =
    Future.fromTry(Try(RiderId(riderIdString)))

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
