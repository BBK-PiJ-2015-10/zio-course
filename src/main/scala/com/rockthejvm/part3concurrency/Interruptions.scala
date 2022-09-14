package com.rockthejvm.part3concurrency

import zio.{Scope, ZIO, ZIOAppArgs, ZIOAppDefault}
import zio._
import com.rockthejvm.utilsScala2.DebugWrapper

import java.util.concurrent.TimeUnit

object Interruptions extends ZIOAppDefault{

  val zioWithTime =
    (
      ZIO.succeed("starting computation").debugThread
      *> ZIO.sleep(2.seconds)
      *> ZIO.succeed(42).debugThread
      )
  .onInterrupt(ZIO.succeed("I was interrupted").debugThread)
  //.ondoNE

  val interruption = for {
    fib <- zioWithTime.fork
    _   <- ZIO.sleep(1.second) *> ZIO.succeed("Interrupting").debugThread *> fib.interrupt  /* This will semantically block this calling fiber*/
    _   <- ZIO.succeed("Interruption successful").debugThread
    result <- fib.join
  } yield result

  val interruption2 = for {
    fib <- zioWithTime.fork
    //_   <- (ZIO.sleep(1.second) *> ZIO.succeed("Interrupting").debugThread *> fib.interrupt).fork  /* This won't block this calling fiber */
    _   <- ZIO.sleep(1.second) *> ZIO.succeed("Interrupting").debugThread *> fib.interruptFork
    _   <- ZIO.succeed("Interruption successful").debugThread
    result <- fib.join
  } yield result

  /*
  Automatic interruptions
   */
  // outliving the parent fiber
  val parentEffect =
    ZIO.succeed("spawning fiber").debugThread *>
    //zioWithTime.fork *>  //child fiber
    zioWithTime.forkDaemon // child of the main application fiber
    ZIO.sleep(1.second) *>
    ZIO.succeed("parent successful").debugThread // done here

  val testOutlivingParent = for {
    parentEffectFib <- parentEffect.fork
    - <- ZIO.sleep(3.seconds)
    - <- parentEffectFib.join
  } yield ()
  // children fibers will be (automatically interrupted) if the parent fiber is completed

 val slowEffect = (ZIO.sleep(2.seconds) *> ZIO.succeed("slow").debugThread).onInterrupt(ZIO.succeed("[slow] interrupted").debugThread)

  val fastEffect = (ZIO.sleep(1.seconds) *> ZIO.succeed("fast").debugThread).onInterrupt(ZIO.succeed("[fast] interrupted").debugThread)

  val aRace = slowEffect.race(fastEffect)

  val testRace = aRace.fork *> ZIO.sleep(3.seconds)

  /**
   * Exercise
   */

    // 1 - implement a timeout function
   // if ZIO successful before timeout return A, or E. If ZIO longer, then interrupt the effect
  def timeout[R,E,A](zio: ZIO[R,E,A], time: Duration) : ZIO[R,E,A] = for {
    fib <- zio.fork
    _   <- (ZIO.sleep(time) *> fib.interrupt).fork
    result <- fib.join
  } yield result

  def timeout_v2[R,E,A](zio: ZIO[R,E,A], time: Duration) : ZIO[R,E,Option[A]] = {
    for {
    fib <- zio.fork
    fib2 <- (ZIO.sleep(time) *> fib.interrupt).fork
    result <- fib.join.map(zioR => Some(zioR)).race(fib2.join.map(ale => None))
  } yield result
  }

  def timeout_v3[R,E,A](zio: ZIO[R,E,A], time: Duration) : ZIO[R,E,Option[A]] =
    timeout(zio,time).foldCauseZIO(
      cause => if (cause.isInterrupted) ZIO.succeed(None) else ZIO.failCause(cause),
      value => ZIO.succeed(Some(value))
    )

  def testTimeout_v2 = timeout_v2(
    ZIO.succeed("Starting...").debugThread *> ZIO.sleep(2.seconds) *> ZIO.succeed("I made it").debugThread,
    1.seconds
  ).debugThread


  val simpleTest: ZIO[Any, RuntimeException, String] =
    for {
    _    <-  ZIO.sleep(1.seconds)
    name <-  ZIO.attempt("Alexis").orElse(ZIO.fail(new RuntimeException("culon"))).debugThread
  } yield name


  override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] = testTimeout_v2
    //timeout(simpleTest,Duration.apply(2,TimeUnit.SECONDS))

}
