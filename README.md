# Supermarket
* 使用Akka, 创建Supermarket Actor, Consumer Actor, Cashier Actor, 其中Cashier使用router创建三个．
* 关于Cashier处理时间间隔，使用Akka Scheduler调度，停顿一段时间向Supermarket发送消息，没有采用sleep, 
* Consumer的时间间隔，也是采用Akka Scheduler，每隔一段时间向Consumer发送Buy事件．
* 定义一个虚拟Actor, Monitor，打印出从Supermarket接收处理结果，并关闭系统
* Supermarket主要初始化商品仓库，使用mutable collection统计处理信息，最后计算出顾客平均等待时间，平均每个商品销售时间，总共销售时间，每个cashier的处理顾客数．

# 使用
`sbt run` 运行任务

`sbt test` 执行测试
