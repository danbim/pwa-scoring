package com.bimschas.pwascoring

import akka.cluster.sharding.typed.scaladsl.EntityTypeKey
import com.bimschas.pwascoring.domain.HeatId
import com.bimschas.pwascoring.service.HeatActor.HeatCommand

package object service {

  val HeatEntityTypeKey: EntityTypeKey[HeatCommand] = EntityTypeKey[HeatCommand]("Heat")

  implicit class HeatIdOps(val heatId: HeatId) extends AnyVal {
    def actorName: String = s"HeatActor_${heatId.number}_${heatId.classifier}"
    def entityId: String = heatId.toString
  }
}
