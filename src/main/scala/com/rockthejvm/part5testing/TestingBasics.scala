package com.rockthejvm.part5testing

import zio._
import zio.test._

case class Person(name: String, age: Int) {

  def spellName: String = name.toUpperCase()

  def saySomthing: UIO[String] = ZIO.succeed(s"Hi, I'm $name")
}

object MyTestSpec extends ZIOSpecDefault {

  // fundamental method

  override def spec: Spec[TestEnvironment with Scope, Any] = test("First Test"){
    val person = Person("Alexander",99)

    // an assertion
    assert(person.spellName)(Assertion.equalTo("ALEXANDER"))

    assertTrue(person.spellName == "ALEXANDER")
    // an assertion
  }

}

object MyFirstEffectTestSpec extends ZIOSpecDefault {
  override def spec: Spec[TestEnvironment with Scope, Any] = test("First spec test"){
    val person= Person("Alexander",101)

    assertZIO(person.saySomthing)(Assertion.equalTo(s"Hi, I'm Alexander"))
    // does not work with asserTrue
    assertZIO(person.saySomthing)(Assertion.assertion("should be a greeting")(gr => gr =="Hi, I'm Alexander"))
    /*
    Assertion examples
    - Assertion.equalTo
    - Assertion.assertion => tests any true value
    - Assertion.fails/failesCause
    - Assertion.dies => expect that the zio dies with a throwable
    - Assertion.isInterrupted => validates an interruption
    Specialized
      - isLeft/isRight
      - isSome/IsNone for Option
      - isSuccess/isFailure
      - isEmpty/nonEmpty for iterables, contains, has*
      - isEmptyString/nonEmptyString/startsWithStrings/matchesRegex
     */


  }
}

object ASuiteSpec extends ZIOSpecDefault {

  override def spec: Spec[TestEnvironment with Scope, Any] =
    suite("Full suite of tests")(
  test("simple test") {
    assertTrue(1 + 3 == 4)
  },
      test("second test"){
        assertZIO(ZIO.succeed("Scala"))(Assertion.hasSizeString(Assertion.equalTo(5)) && Assertion.startsWithString("S"))
      },
      suite("nested suite")(
        test("a nested suite"){
        assert(List(1,2,3))(Assertion.isNonEmpty && Assertion.hasSameElements(List(3,2,1)))
      },
        test("another nested test"){
          assert(List(1,2,3).headOption)(Assertion.equalTo(Some(1)))
        },
        test("failed nested test"){
          assertTrue(1 + 1 == 100)
        }
      )
    )
}

//class TestingBasics {



//}
