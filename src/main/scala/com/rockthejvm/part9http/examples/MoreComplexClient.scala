package com.rockthejvm.part9http.examples


import zio._
import zhttp.service.{ChannelFactory, Client, EventLoopGroup}
import com.rockthejvm.part9http.ziodev.quickstart.users.User.fromJsonDecoderUser
import com.rockthejvm.part9http.ziodev.quickstart.users.User
import zio.json.DecoderOps

import java.util.UUID


//Follow example from https://github.com/C2FO/c2fo-scala/blob/develop/c2fo-eligibility/src/main/scala/com/c2fo/eligibility/services/external/CalculatorAPIClient.scala
// Parse responses from server examples. Json.

object MoreComplexClient {

  val baseUrl = "http://localhost:9090/users"

  // : ZIO[Any,Throwable,User]
  def getUserByUuid(uuid: UUID): ZIO[EventLoopGroup with ChannelFactory, Serializable, Option[User]] = for {
    _  <- ZIO.logInfo(s"Fetching user with uuid $uuid")
    response  <- Client.request(baseUrl+s"/${uuid.toString}")
    eitherUser       <- response.body.asString.map(_.fromJson[User])
    user             <- ZIO.fromEither(eitherUser)
    _  <- ZIO.logInfo(s"Fetched user $user")
  } yield Option(user)


}
