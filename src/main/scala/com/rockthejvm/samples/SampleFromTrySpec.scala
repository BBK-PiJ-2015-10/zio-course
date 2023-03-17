package com.rockthejvm.samples

import zio._
import zio.{Scope, ZIO}
import zio.test.{Assertion, Spec, TestEnvironment, ZIOSpecDefault, assertZIO}

import scala.util.Try

object SampleFromTrySpec extends ZIOSpecDefault {

  val sampleTest = {
    suite ("testing")(
      test("test1") {
        val response = for {
          record  <- ZIO.succeed(other("other"))
        } yield record
        assertZIO(response)(Assertion.isUnit)
      }
    )
  }


  private def other(name: String): Unit = {
    Unsafe.unsafe { implicit unsafe =>
      runtime.unsafe.run(process(name).orElse(ZIO.unit)).getOrThrowFiberFailure()
    }
  }

  private def process(name: String)= {
    ZIO.fromTry(
      Try(
        decode(name)
      )
    )
  }

  private def decode(name: String) = {
    if (name.equals("alex")){
      ()
    } else {
      throw new IllegalArgumentException("you are too young")
    }
  }

  override def spec: Spec[TestEnvironment with Scope, Any] = sampleTest

}
