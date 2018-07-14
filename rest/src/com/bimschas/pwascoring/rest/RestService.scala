package com.bimschas.pwascoring.rest

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import com.bimschas.pwascoring.ContestService
import com.bimschas.pwascoring.domain.HeatId
import com.bimschas.pwascoring.rest.json.ContestJsonSupport

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

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
    } ~ path("contest" / Segment / "contestants") { heatIdString: String =>
      get {
        onSuccess(for {
          heatId <- Future.fromTry(HeatId.parse(heatIdString)) // TODO use Akka Http features to parse into HeatId directly
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
          complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, scoreSheets.toString))
        }
      }
    }

  def startup(): Future[Unit] = {
    bindingFuture.map(_ => println(s"Started web service on http://${config.hostname}:${config.port}"))
  }

  def shutdown(): Future[Unit] = {
    println(s"Shutting down web service on http://${config.hostname}:${config.port}")
    bindingFuture.flatMap(_.unbind()).map(_ => system.terminate())
  }
}

