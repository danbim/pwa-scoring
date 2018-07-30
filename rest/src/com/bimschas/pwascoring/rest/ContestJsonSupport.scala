package com.bimschas.pwascoring.rest

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import com.bimschas.pwascoring.domain.HeatContestants
import com.bimschas.pwascoring.domain.RiderId
import com.bimschas.pwascoring.domain.ScoreSheet
import com.bimschas.pwascoring.domain.ScoreSheets
import com.bimschas.pwascoring.domain.json.DomainJsonSupport
import spray.json.JsValue
import spray.json.RootJsonFormat

trait ContestJsonSupport extends DomainJsonSupport with SprayJsonSupport {

  implicit val contestSpecFormat: RootJsonFormat[ContestSpec] =
    jsonFormat1(ContestSpec.apply)

  implicit val riderIdList: RootJsonFormat[List[RiderId]] =
    listFormat(riderIdFormat)

  implicit val heatContestants: RootJsonFormat[HeatContestants] =
    jsonFormat1(HeatContestants.apply)

  implicit val scoreSheetFormat: RootJsonFormat[ScoreSheet] =
    jsonFormat2(ScoreSheet.apply)

  implicit val scoreSheetsFormat: RootJsonFormat[ScoreSheets] = new RootJsonFormat[ScoreSheets] {
    override def write(obj: ScoreSheets): JsValue = mapFormat[RiderId, ScoreSheet].write(obj.scoreSheetsByRider)
    override def read(json: JsValue): ScoreSheets = ScoreSheets(mapFormat[RiderId, ScoreSheet].read(json))
  }
}
