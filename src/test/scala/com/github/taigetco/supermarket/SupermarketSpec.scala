package com.github.taigetco.supermarket

import scala.concurrent.duration._
import akka.actor.{Props, ActorSystem}
import akka.testkit.{DefaultTimeout, ImplicitSender, TestKit}
import com.typesafe.config.ConfigFactory
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

/**
  * Created by matthewYang on 5/24/16.
  */
class SupermarketSpec extends TestKit(ActorSystem("SupermarketSpec",
  ConfigFactory.parseString(SupermarketSpec.config)))
  with DefaultTimeout with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll{

  val customerRef = system.actorOf(Customer.props, "customer")

  "Customer Actor" must {
    "Respond a TakeOneGoods msg when send OpenUp msg to Customer" in {
      customerRef ! OpenUp
      expectMsg(2000 millis, TakeOneGoods)
      expectNoMsg(500 millis)
      expectMsg(2000 millis, TakeOneGoods)
      customerRef ! SoldOut
      expectNoMsg(2000 millis)
    }
  }

  "Supermarket Actor " must {
    val supermarketRef = system.actorOf(Props(classOf[Supermarket], testActor), "supermarket")
    "Respond with a GoodsWithCashier message or SoldOut when no goods in Supermarket" in {
      supermarketRef ! TakeOneGoods
      expectMsgType[GoodsWithCashier[Goods]](500 millis)
      supermarketRef ! TakeOneGoods
      expectMsgType[GoodsWithCashier[Goods]](500 millis)
      supermarketRef ! TakeOneGoods
      expectMsgType[GoodsWithCashier[Goods]](500 millis)
      supermarketRef ! TakeOneGoods
      expectMsg(500 millis, SoldOut)
    }

    "Send CashierResult msg, no Respond, check goods sold count" in {
      supermarketRef ! CashierResult("a", 5500, 6000)
      expectNoMsg(500 millis)
      supermarketRef ! CashierResult("b", 6500, 7000)
      expectNoMsg(500 millis)
      supermarketRef ! CashierResult("c", 7500, 8000)
      expectMsg(500 millis, Result(500, 7000, 8000, List(1,1,1)))
    }
  }

  "Cashier Actor" must {
    "Send a GoodsTake msg, respond with a CashierResult" in {
      val cashierRef = system.actorOf(Cashier.props(testActor), "cashier")
      cashierRef ! GoodsTake(Apple, 5000L)
      expectMsgType[CashierResult](2500 millis)
    }
  }
}

object SupermarketSpec {
  val config =
    """
      supermarket{
      |  each.goods.inventory = 1
      |  total.goods.inventory = 3
      |  customer.wait {
      |    max.seconds = 2
      |    min.seconds = 1
      |  }
      |  cashier{
      |    wait {
      |      max.seconds = 2
      |      min.seconds = 1
      |    }
      |    num=3
      |  }
      |  routing-logic = "random"
      |  blocking-dispatcher {
      |    executor = "thread-pool-executor"
      |    thread-pool-executor {
      |      fixed-pool-size = 3
      |    }
      |    throughput = 1
      |  }
      |  test.opentime=0
      |}
    """.stripMargin
}
