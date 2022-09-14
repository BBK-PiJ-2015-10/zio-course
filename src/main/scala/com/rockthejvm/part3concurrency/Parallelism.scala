package com.rockthejvm.part3concurrency

import zio.{Exit, Scope, UIO, ZIO, ZIOAppArgs, ZIOAppDefault}
import com.rockthejvm.utilsScala2.DebugWrapper

import scala.io.BufferedSource

object Parallelism extends ZIOAppDefault{

  val meaningOfLife = ZIO.succeed(42)

  val favLang = ZIO.succeed("Scala")

  // combines/zip in a sequential
  val combined = meaningOfLife.zip(favLang)

  // combination is parallel
  val combinedPar = meaningOfLife.zipPar(favLang)

  /*
      - start 2 effects on fibers
      - what if one fails? the other should be interrupted
      - what if one of the effects is interrupted? the entire should be interruped
      - what if the whole thing is interrupted?
   */

  //try a zipPar
  // hint: fork/join/await, interrupt
  def myZipPar[R,E,A,B](zioa: ZIO[R,E,A], ziob: ZIO[R,E,B]) = {
    val exits = for {
      fiba <- zioa.fork
      fibb <- ziob.fork
      exita <- fiba.await
      exitb     <- exita match {
        case Exit.Success(value) => fibb.await
        case Exit.Failure(_) =>  fibb.interrupt
      }
      a    <- fiba.join
      b    <- fibb.join
    } yield (exita,exitb)
    exits.flatMap{
      case (Exit.Success(a),Exit.Success(b)) => ZIO.succeed((a,b))
      case (Exit.Success(_),Exit.Failure(b)) => ZIO.failCause((b))
      case (Exit.Failure(a),Exit.Success(_)) => ZIO.failCause((a))
      case (Exit.Failure(cause1),Exit.Failure(cause2)) => ZIO.failCause(cause1 && cause2)
    }
  }

  val effects: Seq[ZIO[Any, Nothing, Int]] = (1 to 10).map(i => ZIO.succeed(i).debugThread)
  val collectedValues: ZIO[Any, Nothing, Seq[Int]] = ZIO.collectAllPar(effects)  // "the other round is traverse"
  val collectedValuesDiscard: ZIO[Any, Nothing, Unit] = ZIO.collectAllParDiscard(effects)

  // foreach par
  val prinlnParallel: ZIO[Any, Nothing, List[Unit]] = ZIO.foreachPar((1 to 10).toList)(i => ZIO.succeed(println(i)))

  //reduceAllPar, mergeAllPar
  val sumPar = ZIO.reduceAllPar(ZIO.succeed(0),effects)(_ + _)
  val sumPar2 = ZIO.mergeAllPar(effects)(0)(_ + _)

  /*
     - if all effect succeed, all goo
     - one effect fails => everyone is interrupted, error is surface
     - one effect is interruped => everyone else is interruped, error => interruption (for the big expression)
     - if entire effect is interruped => all effects are interrupted
   */

  /**
   * do word counting using parallel
   */
  def countWords(path: String): UIO[Int] =
    ZIO.succeed {
      val source: BufferedSource = scala.io.Source.fromFile(path)
      val nWords = source.getLines().mkString(" ").split(" ").count(_.nonEmpty)
      println(s"Counted $nWords in $path")
      source.close()
      nWords
    }

  def wordsCountParallel(n: Int): UIO[Int] = {
    val effects: Seq[UIO[Int]] =  (1 to n)
      .map(i => s"src/main/resources/testfile$i.txt")
      .map(countWords(_))
    ZIO.collectAllPar(effects).map(_.sum)
  }

  def wordsCountParallel_v2(n: Int): UIO[Int] = {
    val effects: Seq[UIO[Int]] =  (1 to n)
      .map(i => s"src/main/resources/testfile$i.txt")
      .map(countWords(_))
    ZIO.mergeAllPar(effects)(0)(_ + _)
  }

  def wordsCountParallel_v3(n: Int): UIO[Int] = {
    val effects: Seq[UIO[Int]] =  (1 to n)
      .map(i => s"src/main/resources/testfile$i.txt")
      .map(countWords(_))
    ZIO.reduceAllPar(ZIO.succeed(0),effects)(_ + _)
  }


//  def wordsCountParalle(n: Int) = {
//    val ale: ZIO[Any, Nothing, List[Int]] = ZIO.foreachPar((1 to n).toList)(i => countWords(s"src/main/resources/testfile$i.txt"))
//    ZIO.reduceAllPar(ZIO.succeed(0),ale)(_ + _)
//    val cat = (1 to n).map(ref => ZIO.succeed(s"src/main/resources/testfile$ref.txt"))
//    //cat.collectL
//
//  }






  override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] = wordsCountParallel(3).debugThread


}
