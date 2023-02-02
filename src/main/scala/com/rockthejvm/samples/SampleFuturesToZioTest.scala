package com.rockthejvm.samples

import zio.{Scope,ZIO}
import zio.test.{Assertion, Spec, TestEnvironment, ZIOSpecDefault, assertZIO}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

object SampleTester extends ZIOSpecDefault {

  val sampleTest = {
    suite ("culon")(
      test("ale") {
        val response = for {
          record  <- returnDataFromServiceVersion *> ZIO.unit
        } yield record
        assertZIO(response)(Assertion.isUnit)
      }
    )
  }

  def getFuture()(implicit executionContext: ExecutionContext): Future[Int] = {
    aFuture
  }

  def returnDataFromServiceVersion() = {
      ZIO
        .fromFuture(executionContext => getFuture()(executionContext))
        .catchAll(e => ZIO.logError(s"Something went off $e") *> ZIO.unit)
  }


  val aFuture = Future {
    calculateMeaningOfLife
  }

  def calculateMeaningOfLife : Int = {
    Thread.sleep(2000)
    throw new Exception("damn it")
    42
  }



  private def other() = {
    for {
      result <- ZIO.succeed(true)
    } yield()
  }







  override def spec: Spec[TestEnvironment with Scope, Any] = sampleTest

}
