package com.bimschas.pwascoring.domain

import com.bimschas.pwascoring.Heat.HeatCommand

import akka.cluster.sharding.typed.scaladsl.EntityTypeKey

object Implicits {

  implicit class HeatIdOps(val heatId: HeatId) extends AnyVal {
    def actorName: String = s"HeatActor_${heatId.number}_${heatId.classifier}"
    def entityId: String = s"Heat_${heatId.number}_${heatId.classifier}"
    def entityTypeKey: EntityTypeKey[HeatCommand] = EntityTypeKey[HeatCommand]("Heat")
  }
}
