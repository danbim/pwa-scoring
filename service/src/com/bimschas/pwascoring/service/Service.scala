package com.bimschas.pwascoring.service

import com.bimschas.pwascoring.service.Service.ServiceError
import scalaz.zio.Callback
import scalaz.zio.ExitResult.Completed
import scalaz.zio.ExitResult.Failed
import scalaz.zio.IO

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.util.Failure
import scala.util.Success
import scala.util.control.NoStackTrace

object Service {
  case class ServiceError(cause: Throwable) extends Exception with NoStackTrace
}

trait Service {

  protected implicit val ec: ExecutionContext

  protected def io[E, T](op: => Future[Either[E, T]]): IO[Either[ServiceError, E], T] =
    IO.async { callback: Callback[Either[ServiceError, E], T] =>
      op onComplete {
        case Success(Left(e)) => callback(Failed(Right(e)))
        case Success(Right(v)) => callback(Completed(v))
        case Failure(t) => callback(Failed(Left(ServiceError(t))))
      }
    }
}
