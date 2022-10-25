package com.rockthejvm.part9http.examples

import com.rockthejvm.part9http.ziodev.quickstart.users.User
import zhttp.service.{ChannelFactory, EventLoopGroup}
import zio._

object ClientApplicationRunner extends ZIOAppDefault {

  val program = {for {
    _  <- ZIO.logInfo("Starting Client App")
    user       <- ZIO.attempt(User("Alexis",26))
    createdUserUUID <- MoreComplexClient.createUser(user)
    _          <- ZIO.logInfo(s"Received uuid $createdUserUUID")
    maybeUser  <- MoreComplexClient.getUserByUuid(createdUserUUID)
    _          <- ZIO.logInfo(s"Client runner got $maybeUser")
    _  <- ZIO.logInfo("Finishing App")
  } yield ()}.provide(
    EventLoopGroup.auto(),
    ChannelFactory.auto
  )

  override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] = program
}
