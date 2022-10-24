package com.rockthejvm.part9http.examples


import com.rockthejvm.part2effects.ZIOErrorHandling.anAttempt
import zio._
import zio.json._
import zhttp.service.{ChannelFactory, Client, EventLoopGroup}
import com.rockthejvm.part9http.ziodev.quickstart.users.User.{fromJsonDecoderUser, toJsonEncoderUser}
import com.rockthejvm.part9http.ziodev.quickstart.users.User
import zhttp.http.{Body, Method, Response}
import zio.json.DecoderOps

import java.util.UUID


//Follow example from https://github.com/C2FO/c2fo-scala/blob/develop/c2fo-eligibility/src/main/scala/com/c2fo/eligibility/services/external/CalculatorAPIClient.scala
// Parse responses from server examples. Json.

object MoreComplexClient {

  val baseUrl = "http://localhost:9090/users"

  def getUserByUuid(uuid: UUID): ZIO[EventLoopGroup with ChannelFactory, Serializable, Option[User]] = for {
    _  <- ZIO.logInfo(s"Fetching user with uuid $uuid")
    response  <- Client.request(baseUrl+s"/${uuid.toString}")
    user               <- processGetResponse(response)
    _  <- ZIO.logInfo(s"Fetched user $user")
  } yield user

  //userJson    <- user.toJson

  def createUser(user: User): ZIO[EventLoopGroup with ChannelFactory, Throwable, UUID] = {
    for {
      userJson    <-  ZIO.from(user.toJson)
      body        <- ZIO.from(Body.fromString(userJson))
      response    <- Client.request(baseUrl,Method.POST,content = body)
      processedResponse  <- processPostResponse(response)
    } yield processedResponse
  }


  private def processGetResponse(response: Response) = {
    val status  = response.status
    if (status.isClientError){
      ZIO.succeed(None)
    } else {
      for {
        eitherUser    <- response.body.asString.map(_.fromJson[User])
        user              <- ZIO.fromEither(eitherUser).mapError(er => new Exception(er))
      } yield Some(user)
      }
  }

  private def processPostResponse(response: Response) = {
    val status = response.status
    if (status.isSuccess){
      response.body.asString.map(body => UUID.fromString(body))
    } else {
      for {
        error <- response.body.asString
        errorException <- ZIO.fail(new Exception(s"$error"))
      } yield errorException
    }
  }

}
