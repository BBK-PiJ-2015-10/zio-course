package com.rockthejvm.part3concurrency

import com.rockthejvm.utilsScala2.DebugWrapper
import zio.Schedule.WithState
import zio._

object Schedules extends ZIOAppDefault{

  val aZIO = Random.nextBoolean.flatMap { flag =>
    if (flag) ZIO.succeed("fetched value!").debugThread
    else ZIO.succeed("failure..").debugThread *> ZIO.fail("error")

  }

  val oneTimeSchedule = Schedule.once
  val recurrentSchedule = Schedule.recurs(10)
  val fixedInternalSchedule = Schedule.spaced(1.second)

  //exponential backoff
  val exponentialBackoffSchedule = Schedule.exponential(1.second,2.0)
  val fiboSchedule: WithState[(zio.Duration, zio.Duration), Any, Any, zio.Duration] = Schedule.fibonacci(1.second)

  //combinators
  val recurrentAndSpaces = Schedule.recurs(3) && Schedule.spaced(1.second)

  // sequencing
  // ++ concatenation
  val recurrentThenSpaced = Schedule.recurs(3) ++ Schedule.spaced(1.second)

  // Schedules have R = environment, I = input, O = output
  val totalElapsed = Schedule.spaced(1.second) >>> Schedule.elapsed.map(time => println(s"Total time elapsed $time"))



  override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] = aZIO.retry(totalElapsed)


}
