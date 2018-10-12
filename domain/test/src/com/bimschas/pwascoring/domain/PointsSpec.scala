package com.bimschas.pwascoring.domain

import org.scalatest.Matchers
import org.scalatest.WordSpec

class PointsSpec extends WordSpec with DomainGenerators with Matchers {

  "Points.AscendingOrdering" must {
    "sort in ascending order" in {

      val unsorted = sample(shortListGen(pointsGen))
      val sorted = unsorted.sorted(Points.AscendingOrdering)

      sorted.foldLeft(Points.Min) { case (last, current) =>
        last.value <= current.value shouldBe true
        current
      }
    }
  }
}
