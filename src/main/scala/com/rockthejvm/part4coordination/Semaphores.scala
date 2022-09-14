package com.rockthejvm.part4coordination

import zio._
import com.rockthejvm.utilsScala2._


 // n permits
 // acquire, acquireN - can potentially (semantically) block the fiber
// release, releaseN

// example: limiting the number of concurrrent sessions on a server


object Semaphores extends ZIOAppDefault {

  // Sema(1) is a mutex
  val aSemaphore = Semaphore.make(10)

  def doWorkWhileLoggedIn(): UIO[Int] = ZIO.sleep(3.second) *> Random.nextIntBounded(100)

  def login(id: Int, sem: Semaphore) = {
    // similar to object.synchronized {}            acquire / zio  / release
    ZIO.succeed(s"[task $id] waiting to log in").debugThread *> sem.withPermit {
      for {
        // critical section
        _ <- ZIO.succeed(s"[task $id] logged in, working...").debugThread
        res <- doWorkWhileLoggedIn()
        - <-  ZIO.succeed(s"[task $id] done: $res").debugThread
      } yield res
    }
  }

  def loginWeighted(n: Int, sem: Semaphore) = {
    // similar to object.synchronized {}            acquire / zio  / release
    ZIO.succeed(s"[task $n] waiting to log in with $n permits").debugThread *> sem.withPermits(n) {
      for {
        // critical section
        _ <- ZIO.succeed(s"[task $n] logged in, working...").debugThread
        res <- doWorkWhileLoggedIn()
        - <-  ZIO.succeed(s"[task $n] done: $res").debugThread
      } yield res
    }
  }

  def demoSemaphore = for {
    sem  <- Semaphore.make(2)
    f1  <- login(1,sem).fork
    f2  <- login(2,sem).fork
    f3  <- login(3,sem).fork
    _   <- f1.join
    _   <- f2.join
    _   <- f3.join
  } yield ()

  def demoSemaphoreWeighted = for {
    sem  <- Semaphore.make(2)
    f1  <- loginWeighted(1,sem).fork
    f2  <- loginWeighted(2,sem).fork
    f3  <- loginWeighted(3,sem).fork
    _   <- f1.join
    _   <- f2.join
    _   <- f3.join
  } yield ()

  /**
   *
   * @return
   */

    val mySemaphore = Semaphore.make(1)

    val tasks = ZIO.collectAllPar((1 to 10).map { id =>
      for {
        sem <- mySemaphore
        -   <- ZIO.succeed(s"[task $id] waiting to log in").debugThread
        result <- sem.withPermit {
            for {
              _ <- ZIO.succeed(s"[task $id] logged in, working...").debugThread
              res <- doWorkWhileLoggedIn()
              - <-  ZIO.succeed(s"[task $id] done: $res").debugThread
            } yield res
          }
        } yield result
    })

  val tasksFixed = mySemaphore.flatMap { sem =>
    ZIO.collectAllPar((1 to 10).map { id =>
      for {
        -   <- ZIO.succeed(s"[task $id] waiting to log in").debugThread
        result <- sem.withPermit {
          for {
            _ <- ZIO.succeed(s"[task $id] logged in, working...").debugThread
            res <- doWorkWhileLoggedIn()
            - <-  ZIO.succeed(s"[task $id] done: $res").debugThread
          } yield res
        }
      } yield result
    })
  }

  override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] = tasksFixed.debugThread

}
