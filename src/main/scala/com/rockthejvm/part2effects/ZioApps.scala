package com.rockthejvm.part2effects

import zio.{UIO, ZIO}
import zio._

object ZioApps {

  val meaningOfLife: UIO[Int] = ZIO.succeed(43)

  def main(args: Array[String]): Unit = {

    val runtime = Runtime.default
    implicit val trace: Trace = Trace.empty
    Unsafe.unsafe { implicit u =>
      println(runtime.unsafe.run(meaningOfLife))
    }

  }

}

object BetterApp extends ZIOAppDefault {

  override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] =
    //ZioApps.meaningOfLife.flatMap(mal => ZIO.succeed(println(mal)))
  ZioApps.meaningOfLife.debug

}

// not needed 99% of the time
object ManualApp extends ZIOApp {

  override implicit def environmentTag: zio.EnvironmentTag[ManualApp.type] = ???

  override type Environment = this.type

  override def bootstrap: ZLayer[ZIOAppArgs with Scope, Any, ManualApp.type] = ???

  override def run: ZIO[ManualApp.type with ZIOAppArgs with Scope, Any, Any] = ???


}
