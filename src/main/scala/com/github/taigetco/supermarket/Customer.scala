package com.github.taigetco.supermarket

import java.util.concurrent.ThreadLocalRandom
import scala.concurrent.duration._

import akka.actor._

/**
  * Created by matthewYang on 5/24/16.
  */
object Customer {
  def props = Props[Customer]
}

class Customer extends Actor with ActorLogging{
  import context._

  val maxWaitSeconds = system.settings.config.getInt("supermarket.customer.wait.max.seconds")
  val minWaitSeconds = system.settings.config.getInt("supermarket.customer.wait.min.seconds")

  var supermarket: ActorRef = null

  var buyTick: Cancellable = null

  override def receive: Receive = {
    case OpenUp =>
      //every 1 ~ 3 seconds, send Buy event to customer, which indicate Customer buy one Goods
      supermarket = sender()
      buyTick = system.scheduler.schedule(Duration.Zero, randomWaitTime(maxWaitSeconds, minWaitSeconds).seconds)(self ! Buy)
      become(buyGoods)
  }

  def buyGoods: Receive = {
    case Buy =>
      supermarket ! TakeOneGoods
    case GoodsWithCashier(goods, router) =>
      router ! GoodsTake(goods, System.currentTimeMillis)
    case SoldOut =>
      if (buyTick != null) {
        log.info("all goods are taken by customers, stop repeatedly sending Buy message to Customer self")
        buyTick.cancel()
        unbecome()
      }
  }
}