package com.bimschas.pwascoring.rest.json

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.unmarshalling.FromEntityUnmarshaller
import com.bimschas.pwascoring.domain.HeatContestants
import com.bimschas.pwascoring.domain.HeatId
import com.bimschas.pwascoring.domain.JumpScore
import com.bimschas.pwascoring.domain.JumpType
import com.bimschas.pwascoring.domain.Points
import com.bimschas.pwascoring.domain.RiderId
import com.bimschas.pwascoring.domain.ScoreSheet
import com.bimschas.pwascoring.domain.ScoreSheets
import com.bimschas.pwascoring.domain.WaveScore
import spray.json.DefaultJsonProtocol
import spray.json.DeserializationException
import spray.json.JsNumber
import spray.json.JsString
import spray.json.JsValue
import spray.json.RootJsonFormat
import spray.json.deserializationError

trait ContestJsonSupport extends SprayJsonSupport with DefaultJsonProtocol {

  implicit object HeatIdFormat extends RootJsonFormat[HeatId] {
    override def read(json: JsValue): HeatId = json match {
      case JsString(v) => HeatId.parse(v).fold(
        t => deserializationError(s"$v is not a valid HeatId", t),
        id => id
      )
      case v => deserializationError(s"$v is not a valid HeatId")
    }
    override def write(obj: HeatId): JsValue =
      JsString(obj.toString)
  }

  implicit val heatIdUnmarshaller: FromEntityUnmarshaller[HeatId] =
    sprayJsonUnmarshallerConverter(HeatIdFormat)

  implicit val heatIdListUnmarshaller: FromEntityUnmarshaller[List[HeatId]] =
    sprayJsonUnmarshallerConverter(listFormat(HeatIdFormat))

  implicit val heatIdSetUnmarshaller: FromEntityUnmarshaller[Set[HeatId]] =
    sprayJsonUnmarshallerConverter(immSetFormat(HeatIdFormat))

  implicit val pointsFormat: RootJsonFormat[Points] = new RootJsonFormat[Points] {
    override def write(obj: Points): JsValue = JsNumber(obj.value)
    override def read(json: JsValue): Points = json match {
      case JsNumber(value) => Points(value)
      case sthElse => throw DeserializationException(s"Expected a number, got [$sthElse]")
    }
  }

  implicit val waveScoreFormat: RootJsonFormat[WaveScore] =
    jsonFormat1(WaveScore.apply)

  implicit val jumpTypeFormat: RootJsonFormat[JumpType] = new RootJsonFormat[JumpType] {
    override def write(obj: JumpType): JsValue = JsString(obj.toString.toLowerCase())
    override def read(json: JsValue): JumpType = json match {
      case JsString(value) => JumpType.values.collectFirst {
        case jumpType if jumpType.toString.toLowerCase() == value.toLowerCase() => jumpType
      }.get
      case sthElse => throw DeserializationException(s"Expected one of ${JumpType.values.map(_.toString.toLowerCase())}, got [$sthElse]")
    }
  }

  implicit val jumpScoreFormat: RootJsonFormat[JumpScore] =
    jsonFormat2(JumpScore.fromDouble)

  implicit val waveScoreUnmarshaller: FromEntityUnmarshaller[WaveScore] =
    sprayJsonUnmarshallerConverter(waveScoreFormat)

  implicit val jumpScoreUnmarshaller: FromEntityUnmarshaller[JumpScore] =
    sprayJsonUnmarshallerConverter(jumpScoreFormat)

  implicit val riderIdFormat: RootJsonFormat[RiderId] = new RootJsonFormat[RiderId] {
    override def write(obj: RiderId): JsValue = JsString(obj.sailNr)
    override def read(json: JsValue): RiderId = json match {
      case JsString(value) => RiderId.apply(value)
      case sthElse => throw DeserializationException(s"Expected a rider ID, got [$sthElse]")
    }
  }

  implicit val riderIdListUnmarshaller: FromEntityUnmarshaller[List[RiderId]] =
    sprayJsonUnmarshallerConverter(listFormat(riderIdFormat))

  implicit val heatContestantsUnmarshaller: FromEntityUnmarshaller[HeatContestants] =
    jsonFormat1(HeatContestants.apply)

  implicit val scoreSheetFormat: RootJsonFormat[ScoreSheet] =
    jsonFormat2(ScoreSheet.apply)

  implicit val scoreSheetsFormat: RootJsonFormat[ScoreSheets] = new RootJsonFormat[ScoreSheets] {
    override def write(obj: ScoreSheets): JsValue = mapFormat[RiderId, ScoreSheet].write(obj.scoreSheetsByRider)
    override def read(json: JsValue): ScoreSheets = ScoreSheets(mapFormat[RiderId, ScoreSheet].read(json))
  }
}
