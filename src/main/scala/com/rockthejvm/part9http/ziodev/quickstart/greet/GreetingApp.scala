package com.rockthejvm.part9http.ziodev.quickstart.greet

import zhttp.http._

object GreetingApp {

  def apply(): Http[Any,Nothing,Request,Response] =
    Http.collect[Request] {

      // GET /greet?name=:name
      case req@(Method.GET -> !! / "greet" ) if (req.url.queryParams.nonEmpty) =>
        Response.text(s"Hello ${req.url.queryParams("name").mkString(" and ")}!")

      // GET / greet
      case Method.GET -> !! / "greet" => Response.text(s"Hello dog")

      // GET /greet/:name
      case Method.GET -> !! / "greet" / name =>
        Response.text(s"Hello $name!")


    }

}
