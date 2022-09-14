package com.rockthejvm.part2effects

import zio._

import scala.io.StdIn

object ZioEffects {

  /**
   * Simplified ZIO
   *
   */
  case class MyZIO[-R,+E,+A](unsafeRun: R => Either[E,A]) {
    def map[B](f: A => B): MyZIO[R,E,B] = {
      MyZIO(r => unsafeRun(r) match {
        case Left(e) => Left(e)
        case Right(v) => Right(f(v))
      })
    }
    def flatMap[R1 <: R,E1 >: E, B](f: A => MyZIO[R1,E1,B]): MyZIO[R1,E1,B] = {
      MyZIO(r => unsafeRun(r) match {
        case Left(e) => Left(e)
        case Right(v) => f(v).unsafeRun(r)
      }
      )
    }
  }

  val meaningOfLife: ZIO[Any,Nothing,Int] = ZIO.succeed(42)
  val aFailure: ZIO[Any,String,Nothing] = ZIO.fail("Something went wrong")

  // suspension/delay
  val aSuspendedZio : ZIO[Any,Throwable,Int] = ZIO.suspend(meaningOfLife)

  val improvedMOL = meaningOfLife.map(_ * 2)
  val printingMOL = meaningOfLife.flatMap(mol => ZIO.succeed(println(mol)))

  // for comprehensions
  val smallProgram = for {
    _ <- ZIO.succeed(println("whats your name"))
    name <- ZIO.succeed(StdIn.readLine())
    _    <- ZIO.succeed(println(s"Welcome to ZIO, $name"))
  } yield ()

  //zip, zipWith
  val anotherMOL = ZIO.succeed(100)
  val tupledZIO : ZIO[Any,Nothing,(Int,Int)] = meaningOfLife.zip(anotherMOL)
  val combinedZio : ZIO[Any,Nothing,Int] = meaningOfLife.zipWith(anotherMOL)(_ * _)

  // UIO = ZIO[Any,Nothing,A]
  val aUIO : UIO[Int] = ZIO.succeed(99)
  // URIO[R,A] = ZIO[R,Nothing,A]
  val aURIO : URIO[Int,Int] = ZIO.succeed(67)
  // RIO[R,A] = ZIO[R,Throwable,A]
  val anRIO : RIO[Int,Int] = ZIO.succeed(34)
  val aFailedRIO: RIO[Int,Int] = ZIO.fail(new RuntimeException("RIO failed"))

  //Task[A] = ZIO[Any,Throwable,A]
  val aSuccessfulTask : Task[Int] = ZIO.succeed(89)
  val aFailedTask : Task[Int] = ZIO.fail(new RuntimeException("something bad"))

  //IO[E,A]
  val aIO : IO[String,Int] = ZIO.succeed(34)
  val failedIO : IO[String,Int] = ZIO.fail("something is bad")


  /**
   *
    * 1 - sequence two ZIOs and take the value of the last one
   */

  def sequenceTakeLast[R,E,A,B](zioa: ZIO[R,E,A], ziob: ZIO[R,E,B]) : ZIO[R,E,B] = zioa.zipRight(ziob)

  // 2 - sequence two ZIO take value of first one

  def sequenceTakeFirst[R,E,A,B](zioa: ZIO[R,E,A], ziob: ZIO[R,E,B]) : ZIO[R,E,A] = zioa.zipLeft(ziob)

  // 2 - run a ZIO forever
  def runForever[R,E,A](zio: ZIO[R,E,A]): ZIO[R,E,A] = {
    zio.zipRight(runForever(zio))
  }

  val endlessLoop = runForever{
    ZIO.succeed {
      println("running")
      Thread.sleep(1000)
    }
  }

  // 4 - convert the value of a ZIO to something else
  def convert[R,E,A,B](zio: ZIO[R,E,A], value: B): ZIO[R,E,B] = {
//    for {
//      _ <- zio
//    } yield value
    zio.as(value)
  }

  // 5 - discard the value of a ZIO to Unit
  def asUnit[R,E,A](zio: ZIO[R,E,A]): ZIO[R,E,Unit] = {
//    for {
//      _ <- zio
//    } yield ()
    //convert(zio,())
    zio.unit
  }

  // 6 - recursion
  def sum(n: Int): Int =
    if (n == 0) 0
    else n + sum(n - 1)

  def sumZIO(n: Int): UIO[Int] = {
    if (n == 0) {
      ZIO.succeed(0)
    } else {
      //sumZIO(n-1).map(r => r + n)
      for {
        current <- ZIO.succeed(n)
        prevSum <- sumZIO(n-1)
      } yield current + prevSum
    }
  }

  // 7 - fibonacci
  // hint: use ZIO.suspend
  // ZIO.suspendSucceed

  def fibo(n: Int): BigInt  =
    if (n <=2 ) 1
    else fibo(n -1) + fibo(n -2)

  def fiboZIO(n: Int): UIO[BigInt] = {
    if (n <= 2) ZIO.succeed(1)
    else for {
      last <- ZIO.suspendSucceed(fiboZIO(n -1))
      previous <- fiboZIO(n -2)
    } yield last + previous
  }


  def main (args: Array[String]) : Unit = {

    val runtime = Runtime.default
    //implicit val trace: Trace = Trace.empty
    Unsafe.unsafe { implicit u =>

      val firstEffect = ZIO.succeed{
        println("Computing first effect")
        Thread.sleep(1000)
        1
      }

      val secondEffect = ZIO.succeed{
        println("Computing second effect")
        Thread.sleep(1000)
        2
      }

      //val result = sequenceTakeLast(firstEffect,secondEffect)
      //val result = sequenceTakeFirst(firstEffect,secondEffect)
      //val convertResult = convert(firstEffect,80)
      //val result = asUnit(firstEffect)

      val mol = runtime.unsafe.run(fiboZIO(20000))
      println(mol)
    }

  }

}
