package com.github.taigetco

import java.util.concurrent.ThreadLocalRandom

import scala.collection.mutable
import akka.actor._
import akka.routing._

import scala.collection.mutable.ListBuffer

/**
  * Created by matthewYang on 5/23/16.
  */
object Supermarket extends App{
  implicit val system = ActorSystem("SupermarketSystem")

  val supermarketActor = system.actorOf(Props[Supermarket], "supermarket")

  val customerActor = system.actorOf(Customer.props, "customer")

  customerActor.tell(OpenUp, supermarketActor)
}

class Supermarket extends Actor with ActorLogging{
  import context._
  // market open time, which is the selling time
  val openTime = System.currentTimeMillis()

  val eachGoodsInventory = system.settings.config.getInt("supermarket.each.goods.inventory")

  val totalGoodsInventory = system.settings.config.getInt("supermarket.total.goods.inventory")

  val cashierNum = system.settings.config.getInt("supermarket.cashier.num")

  //only support random or round-robin
  val cashierRouter = system.settings.config.getString("supermarket.routing-logic") match {
    case "random"       => actorOf(RandomPool(cashierNum, routerDispatcher = "supermarket.blocking-dispatcher").props(Cashier.props(self)), "cashier")
    case "round-robin"  => actorOf(RoundRobinPool(cashierNum, routerDispatcher = "supermarket.blocking-dispatcher").props(Cashier.props(self)), "cashier")
    case other          => throw new IllegalArgumentException(s"Unknown 'routing-logic': [$other]")
  }

  private val goodsSoldTime = ListBuffer.empty[(Long, Long, Long)]

  //the count of each cashier accepts customer
  private val cashierServiceCount = mutable.HashMap.empty[String, Int]

  private val takenGoodsCount = mutable.HashMap.empty[Goods, Int]

  override def receive: Actor.Receive = {
    case CashierResult(cashierPath, customerBuyTime, cashierProcessTime, cashierEndTime) =>
      goodsSoldTime += ((customerBuyTime, cashierProcessTime, cashierEndTime))
      cashierServiceCount.update(cashierPath, cashierServiceCount.getOrElse(cashierPath, 0) + 1)
      if (totalGoodsInventory == goodsSoldTime.length) {
        outputResult
        system.terminate()
      }
    case TakeOneGoods =>
      val takedGoods = takeGoods()
      if (takedGoods.isDefined){
        sender ! GoodsWithCashier(takedGoods.get, cashierRouter)
      }else{
        sender ! SoldOut
      }
  }

  def outputResult() {
    val (totalSoldTime, totalWaitTime) = countGoodsSoldTime
    log.info(
      s"""
         |customer average wait time: ${totalWaitTime / goodsSoldTime.length} milliseconds
         |goods average sold out time: ${totalSoldTime / goodsSoldTime.length} milliseconds
         |the entire sold out time: $countSoldOutTime milliseconds
         |the count of each cashier served customer: ${countServedCustomer.mkString(", ")}
     """.stripMargin)
  }

  //each goods sold time = cashierEndTime - openTime,
  //each customer wait time = cashierEndTime - cashierProcessTime - customerBuyTime
  def countGoodsSoldTime: (Long, Long) =
    goodsSoldTime.foldLeft((0L, 0L )){ (b, a) =>
        (b._1 + a._3 - openTime, b._2 + a._3 - a._2 - a._1)
      }

  //in goodsSoldTime listBuffer, the last one is the last sold goods
  def countSoldOutTime = goodsSoldTime.last._3 - openTime

  def countServedCustomer: Seq[Int] = cashierServiceCount.values.toSeq

  private def takeGoods(): Option[Goods] = {
    val goods = randomGoods()
    if (takenGoodsCount.getOrElse(goods, 0) == eachGoodsInventory) {
      takenGoodsCount.find(_._2 < eachGoodsInventory).map{a =>
        takenGoodsCount.update(a._1, takenGoodsCount.getOrElse(a._1, 0) + 1)
        a._1
      }
    } else {
      takenGoodsCount.update(goods, takenGoodsCount.getOrElse(goods, 0) + 1)
      Some(goods)
    }
  }

  // '0' stand for Apple, '1'  stand for Macbook, '2' stand for Cookie
  private def randomGoods(): Goods = ThreadLocalRandom.current().nextInt(3) match {
    case 0 =>
      Apple
    case 1 =>
      Macbook
    case 2 =>
      Cookie
  }
}

sealed trait Goods

case object Apple extends Goods

case object Macbook extends Goods

case object Cookie extends Goods

final case class GoodsTake[T <: Goods](goods: T, customerBuyStart: Long)

final case class CashierResult(cashierPath: String, customerBuyStart: Long, cashierProcessTime: Long, cashierEndTime: Long)

final case class GoodsWithCashier[T <: Goods](goods: T, cashierRouter: ActorRef)

case object Buy

case object TakeOneGoods

case object OpenUp

case object SoldOut

