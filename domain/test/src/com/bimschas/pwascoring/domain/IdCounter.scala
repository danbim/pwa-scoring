package com.bimschas.pwascoring.domain

import java.util.concurrent.atomic.AtomicInteger

object IdCounter {
  private val counter: AtomicInteger = new AtomicInteger()
  def next(): Integer = counter.incrementAndGet()
}
