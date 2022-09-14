package com.rockthejvm.part5testing

import zio._
import zio.test._
import zio.test.TestAspect._

import scala.collection.mutable

object SimpleDependencySpec extends ZIOSpecDefault {

  override def spec: Spec[TestEnvironment with Scope, Any] = test("simple dependency"){
    val aZIO: ZIO[Int,Nothing,Int] = ZIO.succeed(42)
    ///assertZIO(aZIO.provide(ZLayer.succeed(10)))(Assertion.equalTo(42))
    assertZIO(aZIO)(Assertion.equalTo(42))
  }.provide(ZLayer.succeed(10))

}

// left on minute 11:00 a.m.

object BusinessLogicSpec extends ZIOSpecDefault {

  abstract class Database[K,V] {
    def get(key: K): Task[V]
    def put(key: K, value: V): Task[Unit]
  }

  object Database {
    def create(url: String): UIO[Database[String,String]] = ???
  }

  // logic under test
  def normalizedUsername(name: String): UIO[String] = ZIO.succeed(name.toUpperCase())

  val mockDatabase = ZIO.succeed(new Database[String,String]{

    val map = mutable.Map[String,String]()

    override def get(key: String): Task[String] = ZIO.attempt(map(key))

    override def put(key: String, value: String): Task[Unit] = ZIO.succeed(map += (key -> value) )

  })

  override def spec: Spec[TestEnvironment with Scope, Any] =
    suite("A user survey application should..")(
    test("normalized user names") {
      val surveyPreliminaryLogic: ZIO[Database[String, String], Throwable, String] = for {
        db  <- ZIO.service[Database[String,String]]
        _   <- db.put("123","alexis")
        username <- db.get("123")
        normalized <- normalizedUsername(username)
      } yield normalized
    assertZIO(surveyPreliminaryLogic)(Assertion.equalTo("ALEXIS"))
    }
  ).provide(ZLayer(mockDatabase))
}

/**
 * build-in test services
 */

object DummyConsoleApplication {

  def welcomeUser(): Task[Unit] =  for {
    _   <- Console.printLine("Please enter your name")
    name   <- Console.readLine("")
    _    <- Console.printLine(s"Welcome $name")
  } yield ()

}

/**
 * built-in services
 *  - console
 *  - clock
 *  - random
 */

object BuiltInTestServicesSpec extends ZIOSpecDefault {

  override def spec: Spec[TestEnvironment with Scope, Any] = suite("Checking built-in test service")(
    test("ZIO console application") {
      val logicUnderTest : Task[Vector[String]] = for {
        _   <- TestConsole.feedLines("Alejandro")
        _   <- DummyConsoleApplication.welcomeUser()
        output   <- TestConsole.output
      } yield output.map(_.trim)
      assertZIO(logicUnderTest)(Assertion.hasSameElements(List("Please enter your name","","Welcome Alejandro")))
    },
    test("ZIO clock" ){
      val parallelEffect = for {
        fiber <- ZIO.sleep(5.minutes).timeout(1.minute).fork
        _     <- TestClock.adjust(1.minute)
        result <- fiber.join
      } yield result

      assertZIO(parallelEffect)(Assertion.isNone)
    },
    test("zio random"){
      val effect = for {
        _    <- TestRandom.feedInts(3,4,1,2)
        value <- Random.nextInt
      } yield value
      assertZIO(effect)(Assertion.equalTo(3))
    }
  )

}


/*
Test aspects
 */

object AspectsSpec extends ZIOSpecDefault {

  def computeMeaningOfLive: UIO[Int] =
    ZIO.sleep(2.seconds) *> ZIO.succeed(42)

  override def spec: Spec[TestEnvironment with Scope, Any] = suite("Checking built-in test service")(
    test("timeout aspect"){
      val effect = for {
        molFib    <- computeMeaningOfLive.fork
        _      <- TestClock.adjust(3.seconds)
        v      <- molFib.join
      } yield v
      assertZIO(effect)(Assertion.equalTo(42))
    } @@eventually @@timed
    ///@@ timeout(10.seconds)
  )
}


// left on 28:09
/*
Aspects:
- timeout(duration)
- eventually - retries until successfully
- nonFlaky(n) - repeat n times, stops at first failure
- repeat(n) -
- retries(n) - retries n times, stops at first success
- debug - print everything on console
- silent - prints nothing
- diagnose(duration) will put a dumb with what happened
- parallel/sequential (aspect of a SUITE)
- ignore
- success - fail all ignored tests
- timed  - measure execution time
- before/beforeAll + after/afterAll
  */

//class TestServices {
//
//}
