package com.bimschas.pwascoring.rest.json

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.unmarshalling.FromEntityUnmarshaller
import com.bimschas.pwascoring.domain.HeatId
import spray.json.DefaultJsonProtocol
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
}
