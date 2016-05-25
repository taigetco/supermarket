package com.github.taigetco.supermarket

import scala.collection.mutable
import akka.actor._
import akka.routing._

import scala.collection.mutable.ListBuffer

/**
  * Created by matthewYang on 5/23/16.
  */
object Supermarket extends App{
  implicit val system = ActorSystem("SupermarketSystem")

  val monitorActor = system.actorOf(Monitor.props, "monitor")

  val supermarketActor = system.actorOf(Props(classOf[Supermarket], monitorActor), "supermarket")

  val customerActor = system.actorOf(Customer.props, "customer")

  customerActor.tell(OpenUp, supermarketActor)
}

class Supermarket(monitorRef: ActorRef) extends Actor with ActorLogging{
  import context._

  // market open time, which is the selling time
  val openTime =
    if (system.settings.config.hasPath("supermarket.test.opentime")) {
      system.settings.config.getLong("supermarket.test.opentime")
    }else {
      System.currentTimeMillis()
    }

  val eachGoodsInventory = system.settings.config.getInt("supermarket.each.goods.inventory")

  val totalInventory = system.settings.config.getInt("supermarket.total.goods.inventory")

  val cashierNum = system.settings.config.getInt("supermarket.cashier.num")

  //only support random or round-robin
  val cashierRouter = system.settings.config.getString("supermarket.routing-logic") match {
    case "random"       => actorOf(
      RandomPool(cashierNum, routerDispatcher = "supermarket.blocking-dispatcher").props(Cashier.props(self)), "cashier")
    case "round-robin"  => actorOf(
      RoundRobinPool(cashierNum, routerDispatcher = "supermarket.blocking-dispatcher").props(Cashier.props(self)),"cashier")
    case other          => throw new IllegalArgumentException(s"Unknown 'routing-logic': [$other]")
  }

  private val goodsSoldTime = ListBuffer.empty[(Long, Long)]

  //the count of each cashier accepts customer
  private val cashierServiceCount = mutable.HashMap.empty[String, Int]

  private val goodsInventory = initInventory

  override def receive: Actor.Receive = {
    case CashierResult(cashierPath, customerBuyTime, cashierEndTime) =>
      goodsSoldTime += ((customerBuyTime, cashierEndTime))
      cashierServiceCount.update(cashierPath, cashierServiceCount.getOrElse(cashierPath, 0) + 1)
      if (totalInventory == goodsSoldTime.length) {
        monitorRef ! outputResult
      }
    case TakeOneGoods =>
      val takenGoods = takeOneGoods()
      if (takenGoods.isDefined){
        sender ! GoodsWithCashier(takenGoods.get, cashierRouter)
      }else{
        sender ! SoldOut
      }
  }

  private def outputResult = {
    val (totalGoodsSoldTime, totalWaitTime) = countGoodsSoldTime(goodsSoldTime.toList, openTime)
    //in goodsSoldTime listBuffer, the last one is the last sold goods
    val totalSoldTime = goodsSoldTime.last._2 - openTime
    val eachCustomerCount = cashierServiceCount.values.toList
    Result(totalWaitTime/totalInventory, totalGoodsSoldTime/totalInventory, totalSoldTime, eachCustomerCount)
  }

  private def takeOneGoods(): Option[Goods] = {
    val goods = randomGoods()
    if (goodsInventory(goods) == 0) {
      goodsInventory.find(_._2 > 0).map{a =>
        goodsInventory.update(a._1, goodsInventory(a._1) - 1)
        a._1
      }
    } else {
      goodsInventory.update(goods, goodsInventory(goods) - 1)
      Some(goods)
    }
  }

  private def initInventory: mutable.HashMap[Goods, Int] = {
    val inventoryMap = mutable.HashMap.empty[Goods, Int]
    goodsMap.values.foreach{goods =>
      inventoryMap.update(goods, eachGoodsInventory)
    }
    inventoryMap
  }
}

