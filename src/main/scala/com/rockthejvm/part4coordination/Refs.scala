package com.rockthejvm.part4coordination

import com.rockthejvm.utilsScala2.DebugWrapper
import zio._

import java.util.concurrent.TimeUnit

object Refs extends ZIOAppDefault {

  val atomicMOL: ZIO[Any,Nothing,Ref[Int]] = Ref.make(42)

  val mol: ZIO[Any, Nothing, Int] = atomicMOL.flatMap{
    ref => ref.get
  }

  val setMol: ZIO[Any, Nothing, Unit] = atomicMOL.flatMap{
    ref => ref.set(100)
  }

  // get and change in one atomic, returns the old value
  val gsMol: ZIO[Any, Nothing, Int] = atomicMOL.flatMap{ ref =>
    ref.getAndSet(500)
  }

  val updatedMol: UIO[Unit] = atomicMOL.flatMap { ref =>
    ref.update(_ * 100)
  }

  val updatedMolWithValue = atomicMOL.flatMap{ ref =>
    ref.updateAndGet(_ * 100)
    // returns the old
    //ref.getAndUpdate(_ * 100)
  }

  // modify
  val modifiedMol: ZIO[Any, Nothing, String] = atomicMOL.flatMap { ref =>
    ref.modify(value => (s"My current value is $value",value))
  }

  // example: distributing work
  def demoConcurrentWorkImpure(): UIO[Unit] = {
    var count = 0

    def task(workload: String): UIO[Unit] = {
      val wordCount = workload.split(" ").length
      for {
        _  <- ZIO.succeed(s"Counting words for: $wordCount").debugThread
        newCount <- ZIO.succeed(count + wordCount)
        _  <- ZIO.succeed(s"new total: $newCount").debugThread
        _  <- ZIO.succeed(count += wordCount)
      } yield ()
    }

    val effects = List("I love ZIO","This Ref thing is cool","Dan writes a LOT of code!").map(task)

    ZIO.collectAllParDiscard(effects)

  }

  def demoConcurrentPure(): UIO[Unit] = {


    def task(workload: String, total:Ref[Int]): UIO[Unit] = {
      val wordCount = workload.split(" ").length
      for {
        _  <- ZIO.succeed(s"Counting words for: $wordCount").debugThread
        newCount <- total.updateAndGet(_ + wordCount)
        _  <- ZIO.succeed(s"new total: $newCount").debugThread
      } yield ()
    }

    for {
      initialCount <- Ref.make(0)
      _             <- ZIO.collectAllParDiscard(
        List("I love ZIO","This Ref thing is cool","Dan writes a LOT of code!").map(load => task(load,initialCount)
      ))
    } yield ()

  }

  /**
   * Exercise
   */

    def tickingClockImpure() : UIO[Unit] = {
      var ticks = 0L

      //print the current time of the system every 1 second + increase the counterc
      def tickingClock: UIO[Unit] = for {
        _  <- ZIO.sleep(1.second)
        _  <- Clock.currentTime(TimeUnit.MILLISECONDS).debugThread
        _  <- ZIO.succeed(ticks +=1)
        _  <- tickingClock
      } yield ()

      //print the total tickCounts every 5 seconds
      def printTicks: UIO[Unit] = for {
        _  <- ZIO.sleep(5.seconds)
        _  <- ZIO.succeed(ticks).debugThread
        _  <- printTicks
      } yield ()

      tickingClock.zipPar(printTicks).unit

    }

  def tickingClockPure() : UIO[Unit] = {

    //print the current time of the system every 1 second + increase the counterc
    def tickingClock(ticks: Ref[Long]): UIO[Unit] = for {
      _  <- ZIO.sleep(1.second)
      _  <- Clock.currentTime(TimeUnit.MILLISECONDS).debugThread
      newUpdaterTicks  <- ticks.updateAndGet(_ + 1)
      //_  <- ZIO.succeed(newUpdaterTicks).debugThread
      _  <- tickingClock(ticks)
    } yield ()

    //print the total tickCounts every 5 seconds
    def printTicks(ticks: Ref[Long]): UIO[Unit] = for {
      _  <- ZIO.sleep(5.seconds)
      _  <- ZIO.succeed(ticks).debugThread
      _  <- printTicks(ticks)
    } yield ()

    for {
      ticks <- Ref.make(0L)
      _ <- tickingClock(ticks).zipPar(printTicks(ticks)).unit
    } yield ()

    Ref.make(0L).flatMap{ ticks =>
      (tickingClock(ticks).zipPar(printTicks(ticks))).unit
    }

  }

  def tickingClockPure_v2() : UIO[Unit] = {

    val ticksRef: UIO[Ref[Long]] = Ref.make(0L)

    //print the current time of the system every 1 second + increase the counterc
    def tickingClock(): UIO[Unit] = for {
      ticks <- ticksRef
      _  <- ZIO.sleep(1.second)
      _  <- Clock.currentTime(TimeUnit.MILLISECONDS).debugThread
      newUpdaterTicks  <- ticks.updateAndGet(_ + 1)
      //_  <- ZIO.succeed(newUpdaterTicks).debugThread
      _  <- tickingClock()
    } yield ()

    //print the total tickCounts every 5 seconds
    def printTicks(): UIO[Unit] = for {
      ticks <- ticksRef
      _  <- ZIO.sleep(5.seconds)
      _  <- ZIO.succeed(ticks).debugThread
      _  <- printTicks()
    } yield ()

    //for {
      //ticks <- Ref.make(0L)
      //_ <- tickingClock().zipPar(printTicks()).unit
    //} yield ()

    (tickingClock.zipPar(printTicks)).unit

//    Ref.make(0L).flatMap{ ticks =>
//      (tickingClock(ticks).zipPar(printTicks(ticks))).unit
//    }

  }

  // update function may be run more than once

  //modify and update will run multiple times if the ref is in use from another fiber

  def demoMultipleUpdates: UIO[Unit] = {

    def task(id: Int, ref:Ref[Int]): UIO[Unit] =
      ref.modify(previous => (println(s"Task $id updating ref at $previous"),id))

    for {
      ref <- Ref.make(0)
      _   <- ZIO.collectAllParDiscard((1 to 10).toList.map(i => task(i,ref)))
    } yield ()



  }







  override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] =
    demoMultipleUpdates
  //tickingClockPure_v2()
    //tickingClockImpure()
  //demoConcurrentPure()
  //demoConcurrentWorkImpure()
  //tickingClockPure()

}
