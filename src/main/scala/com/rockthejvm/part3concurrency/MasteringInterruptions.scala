package com.rockthejvm.part3concurrency

import com.rockthejvm.utilsScala2.DebugWrapper
import zio._

object MasteringInterruptions extends ZIOAppDefault {

  // interruptions
  // fib.interrupt
  // ZIO.race, ZIO.zipPar, ZIO.CollectPar
  // outliving parent fiber

  // manual interruptions

  val aManuallyInterruptedZio = ZIO.succeed("computing...").debugThread *> ZIO.interrupt *> ZIO.succeed(42).debugThread

  val effectWithInterruptionFinalizer  = aManuallyInterruptedZio.onInterrupt(ZIO.succeed("I was interrupted").debugThread)

  // unterruptible
  // payment flow to NOT be interrupted
  val fussyPaymentSystem = (
    ZIO.succeed(println("payment running, Don't cancel me")).debugThread *>
    ZIO.sleep(1.second) *>
    ZIO.succeed("payment completed").debugThread
  ).onInterrupt(ZIO.succeed("MEGA CANCEL OF DOOM!").debugThread)  // don't want this trigger

  val cancellationOfDome: ZIO[Any, Any, Unit] = for {
    fib <- fussyPaymentSystem.fork
    _   <- ZIO.sleep(500.millis) *> fib.interrupt
    _   <- fib.join
  } yield ()

  //ZIO.uninterruptable
  val atomicPayment = ZIO.uninterruptible(fussyPaymentSystem) // makes it uninterruptible
  val atomicPayment_v2: ZIO[Any, Any, String] = fussyPaymentSystem.uninterruptible

  val noCancellationOfDome: ZIO[Any, Any, Unit] = for {
    fib <- atomicPayment.fork
    _   <- ZIO.sleep(500.millis) *> fib.interrupt
    _   <- fib.join
  } yield ()

  // interruptibility if regional
  val zio1: ZIO[Any, Nothing, Int] = ZIO.succeed(1)
  val zio2: ZIO[Any, Nothing, Int] = ZIO.succeed(2)
  val zio3: ZIO[Any, Nothing, Int] = ZIO.succeed(3)

  val zioComposed = (zio1 *> zio2 *> zio3).uninterruptible // All zios are uninterruptible
  val zioComposed1 = (zio1 *> zio2.interruptible *> zio3).uninterruptible  // inner scopes override outer scopes

  // uninterruptibleMask
  // example authentication service
  // input password

  val inputPasswords: ZIO[Any, Nothing, String] = for {
    _  <- ZIO.succeed("Input password:").debugThread
    _  <- ZIO.succeed("(typing the password)").debugThread
    _   <- ZIO.sleep(2.seconds)
    pass <- ZIO.succeed("RockTheJVM1!")
  } yield pass

  def verifyPassword(pw: String): ZIO[Any, Nothing, Boolean] =  for {
    _   <- ZIO.succeed("verifying...").debugThread
    _   <- ZIO.sleep(2.seconds)
    result <- ZIO.succeed(pw == "RockTheJVM1!")
  } yield result

  val authFlow = ZIO.uninterruptibleMask { restore =>
    // EVERYTHING is uninterruptible
    for {
      pw <- restore(inputPasswords.onInterrupt(ZIO.succeed("Authentication time out. Try again later").debugThread))
      // restore the interruptibility flag of this ZIO at the time of the call
      //pw <- inputPasswords.onInterrupt(ZIO.succeed("Authentication time out. Try again later").debugThread)
      verification <- verifyPassword(pw)
      _    <- if (verification) ZIO.succeed("Authentication successful.").debugThread
      else ZIO.succeed("Authentication failed").debugThread
    } yield ()
  }

  val authProgram = for {
    authFib <- authFlow.fork
    _       <- ZIO.sleep(3.seconds) *> ZIO.succeed("Attempting to cancel authentication").debugThread *> authFib.interrupt
    _       <- authFib.join
  } yield ()

  /*
  * Exercise
   */
  val cancelBeforeMol = ZIO.interrupt *> ZIO.succeed(42).debugThread
  val uncancelBeforeMol = ZIO.uninterruptible(ZIO.interrupt *> ZIO.succeed(42).debugThread)

  val authProgram_v2 = for {
    // the interruptible gaps will be covered
    authFiber <- ZIO.uninterruptibleMask(r => authFlow).fork
    _          <- ZIO.sleep(1.seconds) *> ZIO.succeed("Attempting to cancel authentication..").debugThread *> authFiber.interrupt
    -          <- authFiber.join
  } yield ()

  val threeStepProgram = {
    val sequence = ZIO.uninterruptibleMask { restore =>
      for {
        _   <- restore(ZIO.succeed("interruptible 1 ").debugThread *> ZIO.sleep(1.second))
        _  <-  ZIO.succeed("uninterruptible").debugThread *> ZIO.sleep(1.second)
        _   <- restore(ZIO.succeed("interruptible 2").debugThread *> ZIO.sleep(1.second))
      } yield ()
    }

    for {
      fib <- sequence.fork
      _   <- ZIO.sleep(1500.millis) *> ZIO.succeed("Interrupting").debugThread *> fib.interrupt
      _   <- fib.join
    } yield ()


  }




  override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] = threeStepProgram
}
