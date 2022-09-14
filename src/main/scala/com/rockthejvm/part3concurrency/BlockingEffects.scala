package com.rockthejvm.part3concurrency

import com.rockthejvm.utilsScala2.DebugWrapper
import zio._

import java.util.concurrent.atomic.AtomicBoolean

object BlockingEffects extends ZIOAppDefault {

  def blockingTask(n: Int): UIO[Unit] = ZIO.succeed(s"Running blocking task $n").debugThread *>
    ZIO.succeed(Thread.sleep(10000)) *> blockingTask(n)

  val program = ZIO.foreachPar((1 to 100).toList)(blockingTask)
  //thread starvation

  // blocking thread pool
  val aBlockingZIO = ZIO.attemptBlocking {
    println(s"[${Thread.currentThread().getName}] running a long computation")
    Thread.sleep(10000)
    42
  }

  //blocking code cannot usually be interrupted
  val tryInterrupting = for {
    blockingFib <- aBlockingZIO.fork
    -           <- ZIO.sleep(1.second) *> ZIO.succeed("interrupting....").debugThread *> blockingFib.interrupt
    mol         <- blockingFib.join
  } yield ()

  // can use attemptBlockingInterrupt
  // Thread.interrupt -> InterruptedException
  val aBlockingInterruptableZIO = ZIO.attemptBlockingInterrupt{
    println(s"[${Thread.currentThread().getName}] running a long computation")
    Thread.sleep(10000)
    42
  }

  val tryInterruptingInterruptable = for {
    blockingFib <- aBlockingInterruptableZIO.fork
    -           <- ZIO.sleep(1.second) *> ZIO.succeed("interrupting....").debugThread *> blockingFib.interrupt
    mol         <- blockingFib.join
  } yield ()

  // set a flag/switch
  def interruptableBlockingEffect(canceledFlag: AtomicBoolean): Task[Unit] =
    ZIO.attemptBlockingCancelable{ //effect
      (1 to 10000).foreach{ element =>
        if (!canceledFlag.get()){
          println(element)
          Thread.sleep(1000)
        }
      }
    }(ZIO.succeed(canceledFlag.set(true)))  //cancelling/interrupting effect

  val interruptableBlocking = for {
    fib <- interruptableBlockingEffect(new AtomicBoolean(false)).fork
    _   <- ZIO.sleep(2.seconds) *> ZIO.succeed("Interrupting").debugThread *> fib.interrupt
    _   <- fib.join
  } yield ()

  // SEMANTIC blocking - no blocking of threads, descheduling the effect/fiber
  // yield
  val sleep = ZIO.sleep(1.second) // SEMANTICALLY blocking, interruptible

  val sleepingThread = ZIO.succeed(Thread.sleep(1000)) // blocking, non interruptable

  //yield
  val chainedZIO = (1 to 1000).map(i => ZIO.succeed(i)).reduce(_.debugThread *> _.debugThread)

  val yieldingDemo = (1 to 10000).map(i => ZIO.succeed(i)).reduce(_.debugThread *> ZIO.yieldNow *> _.debugThread)



  override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] = {
    //program
    //aBlockingZIO
  //tryInterrupting
  //tryInterruptingInterruptable
  //interruptableBlocking
    //chainedZIO
    yieldingDemo
  }
}
