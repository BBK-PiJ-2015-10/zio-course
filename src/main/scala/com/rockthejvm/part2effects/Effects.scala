package com.rockthejvm.part2effects

import scala.io.StdIn

object Effects {

  //fp

  //local reasoning

  def combine(a: Int, b: Int): Int = a + b

  //referential transparency

  val five = combine(2, 3)
  val five_v2 = 2 + 3
  val five_v3 = 5

  // breaking
  val resultOfPrinting: Unit = println("Learning ZIO")
  val resultOfPrinting_v2: Unit = ()

  //example 2
  var anInt = 0
  val changingInt: Unit = (anInt = 42)
  val changingInt_v2: Unit = ()

  // side effects are inevitable

  /*
    Effect properties
     - the type signatures describes what KIND of computation it will perform
     - the type of signature describes the value that it will produce
     - if side effects are required, construction must be separate from the execution
   */

  /**
   *  - type signature describes the KIND of computation
   *  - type signature says that the computation returns an A, if the computation produce something
   *  - no side effects are needed
   *
   * => Option is an effect
   */
  val anOption: Option[Int] = Option(42)


  /**
   *  - describes an async computation
   *  - it produces a value of type A
   *  - Side effects are required, construction is NOT SEPARATE from execution
   */
  //  import scala.concurrent.ExecutionContext.global
  //   val aFuture: Future[Int] = Future(42)

  case class MyIO[A](unsafeRun: () => A) {
    def map[B](f: A => B): MyIO[B] = MyIO(() => f(unsafeRun()))

    def flatMap[B](f: A => MyIO[B]): MyIO[B] = MyIO(() => f(unsafeRun()).unsafeRun())
  }

//  val anIOWithSideEffects: MyIO[Int] = MyIO(() =>
//    println("producing effect")
//  42
//  )

  /**
   * Create some IO WHICH
   * 1. measure current time of system
   * 2. measure the duration of a computation
   *    - use 1
   *    - use map and flat map
   *      3. create io that reads something from the console and return line as effect
   *      4. print something to the console (e.g. "what's your name"), then read, and then print a welcome message
   *
   * @param args
   */

  val currentTime: MyIO[Long] = MyIO(() => System.currentTimeMillis())

  def measure[A](computation: MyIO[A]): MyIO[(Long, A)] = {
    for {
      startTime <- currentTime
      result <- computation
      endTime <- currentTime
    } yield (endTime - startTime, result)
  }

  def measure2[A](computation: MyIO[A]): MyIO[(Long, A)] = {
    MyIO(() => {
      val startTime = System.currentTimeMillis()
      val result = computation.unsafeRun()
      val endTime = System.currentTimeMillis()
      (endTime - startTime, result)
    }
    )
  }

  def demoMeasurement(): Unit = {
    val computation = MyIO(() => {
      println("Crunching numbers")
      Thread.sleep(1000)
      println("Done")
      42
    })
    println(measure(computation).unsafeRun())
    println(measure2(computation).unsafeRun())
  }

  val readLine: MyIO[String] = MyIO(() => StdIn.readLine())

  def putLn(line: String): MyIO[Unit] = MyIO(() => println(line))

  val program = for {
    _ <- putLn("What is your name")
    name <- readLine
    _ <- putLn(s"Welcome to DEU $name")
  } yield ()

  val readWriteLine: MyIO[Unit] = MyIO(
    () => {
      println("What is your name")
      val lineRead = readLine.unsafeRun()
      println(s"Welcome to Berlin $lineRead")
    }
  )

  def main(args: Array[String]): Unit = {

    //readWriteLine.unsafeRun()

    program.unsafeRun()

    //demoMeasurement()

    //anIOWithSideEffects.unsafeRun()
    //val result : Long = currentTime.unsafeRun()

    //println(String.valueOf(result))


  }

}
