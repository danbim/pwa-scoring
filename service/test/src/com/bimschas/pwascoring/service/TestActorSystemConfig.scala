package com.bimschas.pwascoring.service

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory

object TestActorSystemConfig {

  def apply(): Config =
    apply(TestActorSystemName.next())

  def apply(actorSystemName: String): Config = {

    val hostname = "127.0.0.1"
    val port = TcpPortFactory.newPort()
    val testConfig: Config = ConfigFactory.parseString(
      s"""
         |akka {
         |  remote {
         |    log-remote-lifecycle-events = on
         |    netty.tcp {
         |      hostname = "$hostname"
         |      port = $port
         |    }
         |  }
         |  cluster {
         |    seed-nodes = [
         |      "akka.tcp://$actorSystemName@$hostname:$port"
         |    ]
         |  }
         |  persistence {
         |    journal {
         |      plugin = "akka.persistence.journal.inmem"
         |    }
         |  }
         |}
         |
       """.stripMargin
    )
    testConfig.withFallback(ConfigFactory.load())
  }
}
