package com.bimschas.pwascoring.service

import com.bimschas.pwascoring.domain.DomainGenerators
import org.scalatest.BeforeAndAfterAll
import org.scalatest.EitherValues
import org.scalatest.Matchers
import org.scalatest.OptionValues
import org.scalatest.WordSpec
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.prop.PropertyChecks

import scala.util.control.NonFatal

class SpecBase
    extends WordSpec
    with BeforeAndAfterAll
    with ScalaFutures
    with OptionValues
    with EitherValues
    with Matchers
    with PropertyChecks
    with DomainGenerators {

  protected def withResources[T <: AutoCloseable, V](r: => T)(f: T => V): V = {
    val resource: T = r
    require(resource != null, "resource is null")
    var exception: Throwable = null
    try {
      f(resource)
    } catch {
      case NonFatal(e) =>
        exception = e
        throw e
    } finally {
      closeAndAddSuppressed(exception, resource)
    }
  }

  private def closeAndAddSuppressed(e: Throwable, resource: AutoCloseable): Unit = {
    if (e != null) {
      try {
        resource.close()
      } catch {
        case NonFatal(suppressed) =>
          e.addSuppressed(suppressed)
      }
    } else {
      resource.close()
    }
  }
}
