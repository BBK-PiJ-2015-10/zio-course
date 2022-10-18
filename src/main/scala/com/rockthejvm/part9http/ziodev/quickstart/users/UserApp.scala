package com.rockthejvm.part9http.ziodev.quickstart.users

import zhttp.http._
import zio._
import zio.json._
import com.rockthejvm.part9http.ziodev.quickstart.users.User.{toJsonEncoderUser,fromJsonDecoderUser}

object UserApp {

  def apply(): Http[UserRepo, Throwable, Request, Response] = {

    Http.collectZIO[Request] {

      // POST /users -d '{"name": "John", "age": 35}'
      case req @(Method.POST -> !! / "users") =>
        for {
          u <- req.body.asString.map(_.fromJson[User])
          zioResponse  = u match {
            case Left(e) =>
              ZIO.logInfo(s"Failed to parse the input: $e") *>
              ZIO.succeed(Response.text(e).setStatus(Status.BadRequest))
            case Right(u) =>
              UserRepo.register(u).map(id => Response.text(id))
          }
         response <- zioResponse
        } yield response

      // GET /users/:id
      case Method.GET -> !! / "users" / id =>
         UserRepo.lookup(id)
          .map {
            case Some(user) => Response.json(user.toJson)
            case None => Response.status(Status.NotFound)
          }

      // GET /users
      case Method.GET -> !! / "users" =>
        UserRepo.users.map(response => Response.json(response.toJson))

    }




  }

}
