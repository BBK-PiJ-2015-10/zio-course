package com.rockthejvm.part9http.examples

import zio._
import zhttp.service.{ChannelFactory, Client, EventLoopGroup}


//Source: https://github.com/zio/zio-http/blob/main/zio-http-example/src/main/scala/example/SimpleClient.scala
object SimpleClient extends ZIOAppDefault {


  val url = "http://sports.api.decathlon.com/groups/water-aerobics"

  val program = for {
    res  <- Client.request(url)
    data <- res.body.asString
    _    <- Console.printLine(data)
  } yield ()

  override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] =
  program.provide(EventLoopGroup.auto(),ChannelFactory.auto)

}
