package com.rockthejvm.part9http.examples

import com.rockthejvm.part9http.ziodev.quickstart.users.User
import zhttp.service.{ChannelFactory, EventLoopGroup}
import zio._

import java.util.UUID

object ClientApplicationRunner extends ZIOAppDefault {

  val program = {for {
    _  <- ZIO.logInfo("Starting Client App")
    maybeUser  <- MoreComplexClient.getUserByUuid(UUID.randomUUID())
    //maybeUser  <- MoreComplexClient.getUserByUuid(UUID.fromString("e1aed6e0-f92e-4019-85af-02b3c9ad1c86"))
    _          <- ZIO.logInfo(s"Client runner got $maybeUser")
    _  <- ZIO.logInfo("Finishing App")
  } yield ()}.provide(
    EventLoopGroup.auto(),
    ChannelFactory.auto
  )

  override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] = program
}
