package com.bimschas.pwascoring.journal

import java.io.NotSerializableException
import java.nio.charset.Charset

import akka.serialization.SerializerWithStringManifest
import com.bimschas.pwascoring.domain.ContestEvent
import com.bimschas.pwascoring.domain.ContestPlannedEvent
import com.bimschas.pwascoring.domain.HeatEndedEvent
import com.bimschas.pwascoring.domain.HeatEvent
import com.bimschas.pwascoring.domain.HeatPlannedEvent
import com.bimschas.pwascoring.domain.HeatStartedEvent
import com.bimschas.pwascoring.domain.JumpScoredEvent
import com.bimschas.pwascoring.domain.WaveScoredEvent
import com.bimschas.pwascoring.domain.json.DomainJsonSupport
import com.bimschas.pwascoring.journal.JsonJournalManifestsV1.ContestPlannedEventManifestV1
import com.bimschas.pwascoring.journal.JsonJournalManifestsV1.HeatEndedEventManifestV1
import com.bimschas.pwascoring.journal.JsonJournalManifestsV1.HeatPlannedEventManifestV1
import com.bimschas.pwascoring.journal.JsonJournalManifestsV1.HeatStartedEventManifestV1
import com.bimschas.pwascoring.journal.JsonJournalManifestsV1.JumpScoredEventManifestV1
import com.bimschas.pwascoring.journal.JsonJournalManifestsV1.WaveScoredEventManifestV1
import spray.json.JsonParser
import spray.json.JsonReader
import spray.json.JsonWriter

class JsonJournalSerializerV1 extends SerializerWithStringManifest with DomainJsonSupport {

  override def identifier: Int =
    960890366 // random but fixed

  override def manifest(event: AnyRef): String =
    event match {
      case e: ContestEvent =>
        e match {
          case _: ContestPlannedEvent => ContestPlannedEventManifestV1
        }
      case e: HeatEvent =>
        e match {
          // format: OFF
          case _: HeatPlannedEvent => HeatPlannedEventManifestV1
          case _: HeatStartedEvent => HeatStartedEventManifestV1
          case _: WaveScoredEvent  => WaveScoredEventManifestV1
          case _: JumpScoredEvent  => JumpScoredEventManifestV1
          case _: HeatEndedEvent   => HeatEndedEventManifestV1
          // format: ON
        }
      case _ =>
        throw new NotSerializableException(event.getClass.getSimpleName)
    }

  override def toBinary(event: AnyRef): Array[Byte] =
    event match {
      case e: ContestEvent =>
        e match {
          case e: ContestPlannedEvent => serialize[ContestPlannedEvent](e)
        }
      case e: HeatEvent =>
        e match {
          // format: OFF
          case e: HeatPlannedEvent => serialize[HeatPlannedEvent](e)
          case e: HeatStartedEvent => serialize[HeatStartedEvent](e)
          case e: WaveScoredEvent  => serialize[WaveScoredEvent](e)
          case e: JumpScoredEvent  => serialize[JumpScoredEvent](e)
          case e: HeatEndedEvent   => serialize[HeatEndedEvent](e)
          // format: ON
        }
      case _ =>
        throw new NotSerializableException(event.getClass.getSimpleName)
    }

  override def fromBinary(bytes: Array[Byte], manifest: String): AnyRef =
    manifest match {
      // format: OFF
      case ContestPlannedEventManifestV1 => deserialize[ContestPlannedEvent](bytes)
      case HeatPlannedEventManifestV1    => deserialize[HeatPlannedEvent](bytes)
      case HeatStartedEventManifestV1    => deserialize[HeatStartedEvent](bytes)
      case WaveScoredEventManifestV1     => deserialize[WaveScoredEvent](bytes)
      case JumpScoredEventManifestV1     => deserialize[JumpScoredEvent](bytes)
      case HeatEndedEventManifestV1      => deserialize[HeatEndedEvent](bytes)
      // format: ON
      case _ => throw new IllegalArgumentException(s"Unsupported manifest $manifest")
    }

  private def deserialize[T](event: Any)(implicit reader: JsonReader[T]): T =
    JsonParser(new String(event.asInstanceOf[Array[Byte]], charset)).convertTo[T]

  private def serialize[T](event: T)(implicit writer: JsonWriter[T]): Array[Byte] =
    writer.write(event).toString().getBytes(charset)

  private val charset: Charset = Charset.forName("UTF-8")
}
