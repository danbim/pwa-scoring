package com.bimschas.pwascoring.model

import akka.cluster.sharding.typed.scaladsl.EntityTypeKey
import com.bimschas.pwascoring.Heat.HeatCommand

object Implicits {

  implicit class HeatIdOps(val heatId: HeatId) extends AnyVal {
    def actorName: String = s"HeatActor_${heatId.number}_${heatId.classifier}"
    def entityId: String = s"Heat_${heatId.number}_${heatId.classifier}"
    def entityTypeKey: EntityTypeKey[HeatCommand] = EntityTypeKey[HeatCommand]("Heat")
  }
}
