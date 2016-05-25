package com.github.taigetco.supermarket

import akka.actor.{Props, Actor, ActorLogging}

/**
  * output result, and shutdown system
  * Created by matthewYang on 5/24/16.
  */

object Monitor {
  def props = Props[Monitor]
}

class Monitor extends Actor with ActorLogging{
    override def receive: Receive = {
      case Result(perWaitTime, perSoldTime, totalSoldTime, eachCustomerCount) =>
        log.info(
          s"""
             |customer average wait time: $perWaitTime milliseconds
             |goods average sold out time: $perSoldTime milliseconds
             |the entire sold out time: $totalSoldTime milliseconds
             |the count of each cashier served customer: ${eachCustomerCount.mkString(", ")}
     """.stripMargin)
        context.system.terminate()
    }
}
