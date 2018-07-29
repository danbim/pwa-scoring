package com.bimschas.pwascoring.app

object ShutdownHooks {

  def register(hook: => AnyRef): Unit = {
    Runtime.getRuntime.addShutdownHook(new Thread("shutdown-hook-thread") {
      override def run(): Unit = hook
    })
  }
}
