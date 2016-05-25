package com.github.taigetco.supermarket

import scala.concurrent.duration._

import akka.actor.{ActorLogging, Actor, Props, ActorRef}

/**
  * Created by matthewYang on 5/24/16.
  */
object Cashier {
  def props(supermarket: ActorRef) = Props(classOf[Cashier], supermarket)
}

class Cashier(supermarket: ActorRef) extends Actor with ActorLogging{
  import context._

  val maxWaitSeconds = system.settings.config.getInt("supermarket.cashier.wait.max.seconds")
  val minWaitSeconds = system.settings.config.getInt("supermarket.cashier.wait.min.seconds")

  def receive: Receive = {
    case GoodsTake(goods, customerBuyStart) =>
      val waitTime = randomWaitTime(maxWaitSeconds, minWaitSeconds)
      //use Actor path as identifier for cashier
      val cashierPath = self.path.name
      system.scheduler.scheduleOnce(waitTime.seconds){
        log.info(s"cachier $cashierPath processing ${goods}")
        supermarket ! CashierResult(cashierPath, customerBuyStart, System.currentTimeMillis)
      }
  }

}