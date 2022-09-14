package com.rockthejvm.part4coordination

import com.rockthejvm.utilsScala2.DebugWrapper
import zio._
import scala.collection.immutable.Queue

abstract class Mutex {

  def acquire: UIO[Unit]

  def release: UIO[Unit]

}

object Mutex {

  type Signal = Promise[Nothing,Unit]

  case class State(locked: Boolean, waiting: Queue[Signal])

  val unlocked= State(false,Queue())


  def createInterruptibleMutex(state: Ref[State]) = new Mutex {

    override def acquire: UIO[Unit] = ZIO.uninterruptibleMask{ restore =>

      Promise.make[Nothing,Unit].flatMap { promise =>

        val cleanUp: UIO[Unit] = state.modify{
          case State(flag,waiting) =>
            val newWaiting = waiting.filterNot(_ eq promise)
            // block only if newWaiting != waiting => release mutex
            val wasBlocked = newWaiting != waiting
            val decision = if (wasBlocked) ZIO.unit else release
            decision -> State(flag,newWaiting)
        }.flatten

      state.modify {
        case State(false,_) => ZIO.unit -> State(locked = true,Queue())
        case State(true,waiting) => restore(promise.await).onInterrupt(
          cleanUp
        ) -> State(true,waiting.enqueue(promise))
      }.flatten

    } }

    override def release: UIO[Unit] = state.modify {
      case State(false, _) => ZIO.unit -> unlocked
      case State(true,waiting) =>
        if (waiting.isEmpty)   ZIO.unit -> unlocked
        else {
          val (promise,restOfPromises) =  waiting.dequeue
          promise.succeed(()).unit -> State(true,restOfPromises)
        }
    }.flatten

  }

  def make: UIO[Mutex] = Ref.make(unlocked).map { state =>
    createInterruptibleMutex(state)
  }


}

object MutexPlayground extends ZIOAppDefault {


  def workInCriticalRegion(): UIO[Int] =
    ZIO.sleep(1.second) *> Random.nextIntBounded(100)

  def demoNonLockingTasks() =
    ZIO.collectAllParDiscard((1 to 10).toList.map(i =>  for {
      - <- ZIO.succeed(s"[task ${i}] working...").debugThread
      result <- workInCriticalRegion()
      _   <- ZIO.succeed(s"[task ${i}] get result: $result").debugThread
    } yield ()
    ))

  def createTask(id: Int, mutex: Mutex): UIO[Int] = {

    val task = for {
    _  <- ZIO.succeed(s"[task $id waiting for mutex...").debugThread
    _ <- mutex.acquire
    _  <- ZIO.succeed(s"[task $id mutex acquired, working...").debugThread
    result <- workInCriticalRegion().onInterrupt(mutex.release)
    _   <- ZIO.succeed(s"[task ${id}] get result: $result, releasing mutex").debugThread
    _ <- mutex.release
  } yield result

    task
      .onInterrupt(ZIO.succeed(s"[task $id] was interrupted").debugThread)
      .onError(cause => ZIO.succeed(s"[task $id] ended in error: $cause"))

  }

  def demoLockingTasks(): ZIO[Any, Nothing, Unit] =
    for {
      mutex  <-  Mutex.make
      -        <- ZIO.collectAllParDiscard((1 to 10).toList.map(i => createTask(i,mutex)))
    } yield ()


  def createInterruptingTask(id: Int, mutex: Mutex) : UIO[Int] = {
    if (id %2 == 0)
      createTask(id,mutex)
    else for {
      fib <- createTask(id,mutex).fork
      -   <- ZIO.sleep(2500.millis) *> ZIO.succeed(s"Interrupting task $id").debugThread *> fib.interrupt
       result <- fib.join
    } yield result
  }

  def demoInterruptingTasks() = for {
    mutex <- Mutex.make
    fib1   <- createInterruptingTask(1,mutex).fork
    fib2   <- createInterruptingTask(2,mutex).fork
    fib3   <- createInterruptingTask(3,mutex).fork
    fib4   <- createInterruptingTask(4,mutex).fork
    fib5   <- createInterruptingTask(5,mutex).fork
    fib6   <- createInterruptingTask(6,mutex).fork
    fib7   <- createInterruptingTask(7,mutex).fork
    fib8   <- createInterruptingTask(9,mutex).fork
    fib9   <- createInterruptingTask(9,mutex).fork
    fib10   <- createInterruptingTask(10,mutex).fork
    _      <- fib1.await
    _      <- fib2.await
    _      <- fib3.await
    _      <- fib4.await
    _      <- fib5.await
    _      <- fib6.await
    _      <- fib7.await
    _      <- fib8.await
    _      <- fib9.await
    _      <- fib10.await
  } yield ()



  override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] = demoInterruptingTasks()


}