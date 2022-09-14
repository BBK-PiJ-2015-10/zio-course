package com.rockthejvm.part3concurrency

import zio.{Exit, Fiber, Scope, UIO, URIO, ZIO, ZIOAppArgs, ZIOAppDefault}
import com.rockthejvm.utilsScala2.DebugWrapper

import java.io.{File, FileWriter}

object Fibers extends ZIOAppDefault{

  val meaningOfLife = ZIO.succeed(42)
  val favLang = ZIO.succeed("Scala")

  // Fiber - lightweight thread
  def createFiber: Fiber[Throwable,String] = ???

  def sameThreadIo = for {
    mol <- meaningOfLife.debugThread
    lang <- favLang.debugThread
  } yield (mol,lang)

  def differentThreadIo = for {
    mol <- meaningOfLife.debugThread.fork
    lang <- favLang.debugThread.fork
  } yield (mol,lang)

  val meaningOfLifeFiber: URIO[Any, Fiber[Nothing, Int]] = meaningOfLife.fork

  // join a fiber
  def runOnAnotherThread[R,E,A](zio: ZIO[R,E,A]) = for {
    fib <- zio.fork
    result <- fib.join
  } yield result

  // waiting a fiber
  def runOnAnotherThread_v2[R,E,A](zio: ZIO[R,E,A]) = for {
    fib <- zio.fork
    result <- fib.await
  } yield result match {
    case Exit.Success(value) => s"Succeeded with $value"
    case Exit.Failure(cause) => s"Failed with $cause"
  }

  // poll - peek at the result of the fiber RIGHT NOW, without blocking
  val peekFiber = for {
    fib <- ZIO.attempt{
      Thread.sleep(1000)
      42
    }.fork
    result <- fib.poll
  } yield result

  // compose fibers
  // zip
  val zippedFibers: ZIO[Any, Nothing, (String, String)] = for {
    fib1 <- ZIO.succeed("Result from fiber 1").debugThread.fork
    fib2 <- ZIO.succeed("Result from fiber 2").debugThread.fork
    fiber = fib1.zip(fib2)
    tuple <- fiber.join
  } yield tuple

  val zipperFibers_v2 : ZIO[Any, Nothing, (String, String)] = for {
    fib1 <- ZIO.succeed("Result from fiber1").debugThread.fork
    fib2 <- ZIO.succeed("Result from fiber2").debugThread.fork
    fiber <- zipFibers(fib1,fib2)
    tuple  <- fiber.join
  } yield tuple

  // orEsle
  val chainedFiber: ZIO[Any, Nothing, String] = for {
    fiber1 <- ZIO.fail("not good!").debugThread.fork
    fiber2 <- ZIO.succeed("Rock the JVM").debugThread.fork
    fiber = fiber1.orElse(fiber2)
    message <- fiber.join
  } yield message


  /**
   * zip two fibers without using the zip, using instead fork and join (join, flatMap, combinators)
   * hint: create a fiber that wait for both
   */
   //def zipZibers[E,A,B](fiber1: Fiber[E,A],fiber2: Fiber[E,B]): ZIO[Any,Nothing,Fiber[E,(A,B)]] = {
   def zipFibers[E,A,B](fiber1: Fiber[E,A],fiber2: Fiber[E,B]) = {
     val finalEffect = for {
       v1 <- fiber1.join
       v2 <- fiber2.join
     } yield (v1,v2)
     finalEffect.fork
   }

  //def chainFibers[E,A](fiber1: Fiber[E,A], fiber2: Fiber[E,A]): ZIO[Any,Nothing,Fiber[E,A]] =
  def chainFibers[E,A](fiber1: Fiber[E,A], fiber2: Fiber[E,A]) : ZIO[Any,Nothing,Fiber[E,A]] = {
    fiber1.join.orElse(fiber2.join).fork
  }

  //distributing a task in between many fibers
  def generateRandomFile(path: String): Unit = {
    val random = scala.util.Random
    val chars = 'a' to 'z'
    val nWords = random.nextInt(2000)
    val content = (1 to nWords)
      .map(_ => (1 to  random.nextInt(10)).map(_ => chars(random.nextInt(26))).mkString)
      .mkString(" ")
    val writer = new FileWriter(new File(path))
    writer.write(content)
    writer.flush()
    writer.close()
  }

  def countWords(path: String): UIO[Int] =
    ZIO.succeed{
      val source = scala.io.Source.fromFile(path)
      val nwords = source.getLines().mkString(" ").split(" ").count(_.nonEmpty)
      source.close()
      nwords
    }

  def wordsCountParalle(n: Int) = {
    val effects: Seq[ZIO[Any,Nothing, Int]] = (1 to n)
      .map(i => s"src/main/resources/testfile$i.txt")
      .map(countWords)
      .map(_.fork) // list of effects returning fibers
      .map(fiberEffect => fiberEffect.flatMap(_.join))
    effects.reduce { (zioa,ziob) => for {
      a <- zioa
      b <- ziob
    } yield a + b
  }}


//  def zipFibersGeneral[E,E1 <: E,E2 <: E,A,B](fiber1: Fiber[E1,A],fiber2: Fiber[E2,B]) = {
//    val finalEffect = for {
//      v1 <- fiber1.join
//      v2 <- fiber2.join
//    } yield (v1,v2)
//    finalEffect.fork
//  }


  override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] =
    wordsCountParalle(3).debugThread
    //ZIO.succeed(generateRandomFile("src/main/resources/testfile1.txt"))
    //zipperFibers_v2.debugThread
    //runOnAnotherThread_v2(meaningOfLife).debugThread
}
