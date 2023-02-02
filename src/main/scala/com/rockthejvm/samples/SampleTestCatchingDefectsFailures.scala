package com.rockthejvm.samples

import zio.{Scope, ZIO}
import zio.test.{Assertion, Spec, TestEnvironment, ZIOSpecDefault, assertZIO}

object SampleTestCatchingDefectsFailures extends ZIOSpecDefault {

  val sampleTest = {
    suite ("testing")(
      test("test1") {
        val response = for {
          record  <- getHostess(15)
        } yield record
        assertZIO(response)(Assertion.isUnit)
      }
    )
  }

  def getHostess(participantAge: Int) = {
        getLottery(participantAge).catchAllCause{ cause =>
          ZIO.logError(s"Dog is $cause")
      }
  }

  def getLottery(number: Int) = {
        if (number > 20){
          ZIO.unit
        } else {
          if  (number < 10) {
            ZIO.fail(new IllegalArgumentException(s"you young boy, always a loser"))
          } else {
            ZIO.die(new Exception("in your dreams only"))
          }
        }
      }

  override def spec: Spec[TestEnvironment with Scope, Any] = sampleTest

}
