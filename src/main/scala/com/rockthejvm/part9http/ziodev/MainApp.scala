package com.rockthejvm.part9http.ziodev

import com.rockthejvm.part9http.ziodev.quickstart.greet.GreetingApp
import com.rockthejvm.part9http.ziodev.quickstart.counter.CounterApp
import zhttp.http._
import zhttp.http.middleware.Cors.CorsConfig
import zhttp.service.Server
import zio._

//Source: https://github.com/zio/zio-quickstart-restful-webservice
object MainApp extends ZIOAppDefault {

  override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] = Server.start(
    port = 8080,
    http = GreetingApp() ++ CounterApp()
  ).provide(
    ZLayer.fromZIO(Ref.make((0)))
  )

}
