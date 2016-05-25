package com.github.taigetco

import java.util.concurrent.ThreadLocalRandom

import akka.actor.ActorRef

import scala.collection.mutable.ListBuffer

/**
  * Created by matthewYang on 5/25/16.
  */
package object supermarket {

  sealed trait Goods

  case object Apple extends Goods

  case object Macbook extends Goods

  case object Cookie extends Goods

  final case class GoodsTake[T <: Goods](goods: T, customerBuyStart: Long)

  final case class CashierResult(cashierPath: String, customerBuyStart: Long, cashierEndTime: Long)

  final case class GoodsWithCashier[T <: Goods](goods: T, cashierRouter: ActorRef)

  final case class Result(perSoldTime: Long, perWaitLong: Long, totalSoldTime: Long, servedCount: List[Int])

  case object Buy

  case object TakeOneGoods

  case object OpenUp

  case object SoldOut

  // '0' stand for Apple, '1'  stand for Macbook, '2' stand for Cookie
  val goodsMap = Map(0 -> Apple, 1 -> Macbook, 2 -> Cookie)

  def randomWaitTime(maxWaitSeconds: Int, minWaitSeconds: Int) =
    ThreadLocalRandom.current().nextInt(maxWaitSeconds - minWaitSeconds + 1) + minWaitSeconds

  def randomGoods(): Goods = goodsMap(ThreadLocalRandom.current().nextInt(3))

  //each goods sold time = cashierEndTime - openTime,
  //each customer wait time = cashierEndTime - customerBuyTime
  def countGoodsSoldTime(goodsSoldTime: List[(Long, Long)], openTime: Long): (Long, Long) =
    goodsSoldTime.foldLeft((0L, 0L )){ (b, a) =>
      (b._1 + a._2 - openTime, b._2 + a._2 - a._1)
    }
}
