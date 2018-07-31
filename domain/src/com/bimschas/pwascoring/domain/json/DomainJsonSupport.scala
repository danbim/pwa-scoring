package com.bimschas.pwascoring.domain.json

import com.bimschas.pwascoring.domain.ContestPlannedEvent
import com.bimschas.pwascoring.domain.HeatContestants
import com.bimschas.pwascoring.domain.HeatEndedEvent
import com.bimschas.pwascoring.domain.HeatEvent
import com.bimschas.pwascoring.domain.HeatId
import com.bimschas.pwascoring.domain.HeatPlannedEvent
import com.bimschas.pwascoring.domain.HeatStartedEvent
import com.bimschas.pwascoring.domain.JumpScore
import com.bimschas.pwascoring.domain.JumpScoredEvent
import com.bimschas.pwascoring.domain.JumpType
import com.bimschas.pwascoring.domain.Points
import com.bimschas.pwascoring.domain.RiderId
import com.bimschas.pwascoring.domain.WaveScore
import com.bimschas.pwascoring.domain.WaveScoredEvent
import spray.json.DefaultJsonProtocol
import spray.json.DeserializationException
import spray.json.JsNumber
import spray.json.JsString
import spray.json.JsValue
import spray.json.JsonFormat
import spray.json.JsonWriter
import spray.json.RootJsonFormat
import spray.json.deserializationError

trait DomainJsonSupport extends DefaultJsonProtocol {

  implicit val heatIdFormat: JsonFormat[HeatId] = new JsonFormat[HeatId] {
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

  implicit val riderIdFormat: JsonFormat[RiderId] = new JsonFormat[RiderId] {
    override def write(obj: RiderId): JsValue = JsString(obj.sailNr)
    override def read(json: JsValue): RiderId = json match {
      case JsString(value) => RiderId.apply(value)
      case sthElse => throw DeserializationException(s"Expected a rider ID, got [$sthElse]")
    }
  }

  implicit val pointsFormat: JsonFormat[Points] = new JsonFormat[Points] {
    override def write(obj: Points): JsValue = JsNumber(obj.value)
    override def read(json: JsValue): Points = json match {
      case JsNumber(value) => Points(value)
      case sthElse => throw DeserializationException(s"Expected a number, got [$sthElse]")
    }
  }

  implicit val jumpTypeFormat: JsonFormat[JumpType] = new JsonFormat[JumpType] {
    override def write(obj: JumpType): JsValue = JsString(obj.toString.toLowerCase())
    override def read(json: JsValue): JumpType = json match {
      case JsString(value) =>
        JumpType.values.collectFirst {
          case jumpType if jumpType.toString.toLowerCase() == value.toLowerCase() => jumpType
        }.orElse(
          throw DeserializationException(s"Expected one of ${JumpType.values.map(_.toString.toLowerCase())}, got [$value]")
        ).get
      case sthElse =>
        throw DeserializationException(s"Expected one of ${JumpType.values.map(_.toString.toLowerCase())}, got [$sthElse]")
    }
  }

  // format: OFF
  implicit val contestPlannedEvent:    RootJsonFormat[ContestPlannedEvent] = jsonFormat1(ContestPlannedEvent.apply)
  implicit val jumpScoreFormat:        RootJsonFormat[JumpScore]           = jsonFormat2(JumpScore.apply)
  implicit val jumpScoredEventFormat:  RootJsonFormat[JumpScoredEvent]     = jsonFormat3(JumpScoredEvent.apply)
  implicit val waveScoreFormat:        RootJsonFormat[WaveScore]           = jsonFormat1(WaveScore.apply)
  implicit val waveScoredFormat:       RootJsonFormat[WaveScoredEvent]     = jsonFormat3(WaveScoredEvent.apply)
  implicit val heatContestantsFormat:  RootJsonFormat[HeatContestants]     = jsonFormat1(HeatContestants.apply)
  implicit val heatPlannedEventFormat: RootJsonFormat[HeatPlannedEvent]    = jsonFormat2(HeatPlannedEvent.apply)
  implicit val heatStartedEventFormat: RootJsonFormat[HeatStartedEvent]    = jsonFormat1(HeatStartedEvent.apply)
  implicit val heatEndedEventFormat:   RootJsonFormat[HeatEndedEvent]      = jsonFormat1(HeatEndedEvent.apply)
  // format: ON

  def toJson(heatEvent: HeatEvent): JsValue =
    heatEvent match {
      // format: OFF
      case event: HeatPlannedEvent => implicitly[JsonWriter[HeatPlannedEvent]].write(event)
      case event: HeatStartedEvent => implicitly[JsonWriter[HeatStartedEvent]].write(event)
      case event: JumpScoredEvent  => implicitly[JsonWriter[JumpScoredEvent]].write(event)
      case event: WaveScoredEvent  => implicitly[JsonWriter[WaveScoredEvent]].write(event)
      case event: HeatEndedEvent   => implicitly[JsonWriter[HeatEndedEvent]].write(event)
      // format: ON
    }
}
