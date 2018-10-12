package com.bimschas.pwascoring.service

import java.io.IOException
import java.net.InetSocketAddress
import java.nio.channels.ServerSocketChannel
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.IntUnaryOperator

import scala.annotation.tailrec

class TcpPortFactory(firstPort: Int, count: Int, maxRetries: Int) {
  private val portCounter = new AtomicInteger(0)

  /** Tries to find a free throw-away TCP port for tests. */
  def newPort(): Int = newAddress().getPort

  /** Tries to find a free throw-away TCP port for tests. */
  def newAddress(hostname: String = "127.0.0.1"): InetSocketAddress = {
    def nextPort(): Int =
      firstPort + portCounter.getAndUpdate(new IntUnaryOperator {
        override def applyAsInt(counter: Int): Int = (counter + 1) % count
      })

    def isFree(port: Int): Boolean = {
      val serverSocket = ServerSocketChannel.open().socket()

      try {
        serverSocket.bind(new InetSocketAddress(hostname, port))
        true
      } catch {
        case _: IOException => false // port probably already in use
      } finally serverSocket.close()
    }

    @tailrec def alloc(retries: Int): InetSocketAddress = {
      val candidate = nextPort()
      if (isFree(candidate)) new InetSocketAddress(hostname, candidate)
      else if (retries > 0) alloc(retries - 1)
      else throw new RuntimeException(s"Could not find free port after $maxRetries")
    }

    alloc(maxRetries)
  }
}

object TcpPortFactory extends TcpPortFactory(10000, 30000, 100)
