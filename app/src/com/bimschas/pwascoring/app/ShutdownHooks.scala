package com.bimschas.pwascoring.app

object ShutdownHooks {

  def register(hook: => Any): Unit = {
    Runtime.getRuntime.addShutdownHook(new Thread("shutdown-hook-thread") {
      override def run(): Unit = {
        hook
        ()
      }
    })
  }
}
