package com.bimschas.pwascoring.domain

sealed trait JumpType
case object BackLoop extends JumpType
case object FrontLoop extends JumpType
case object TableTop extends JumpType

object JumpType {

  val values: Set[JumpType] = Set(BackLoop, FrontLoop, TableTop)

  def fromString(jumpTypeString: String): JumpType =
    values.collectFirst {
      case jumpType if jumpType.toString.toLowerCase() == jumpTypeString.toLowerCase() => jumpType
    }.get
}