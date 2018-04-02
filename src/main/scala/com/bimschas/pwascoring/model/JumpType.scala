package com.bimschas.pwascoring.model

sealed trait JumpType
case object TableTop extends JumpType
case object FrontLoop extends JumpType
case object BackLoop extends JumpType
