package com.github.taigetco

import java.util.concurrent.ThreadLocalRandom
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
  override def receive: Receive = {
    case GoodsTake(goods, customerBuyStart) =>
      //process time, random 5 ~ 10 seconds
      val processTime = (ThreadLocalRandom.current().nextInt(6) + 5).seconds.toMillis
      //use Actor path as identifier for cashier
      val cashierPath = self.path.name
      system.scheduler.scheduleOnce(processTime.milliseconds){
        log.info(s"cachier $cashierPath processing ${goods} cost ${processTime} milliseconds")
        supermarket ! CashierResult(cashierPath, customerBuyStart, processTime, System.currentTimeMillis)
      }
  }
}