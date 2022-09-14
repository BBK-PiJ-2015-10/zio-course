package com.rockthejvm.part5testing

/*
Need to have:
"dev.zio" %% "zio-test-junit" % zioVersion
testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")
 */


import zio._
import zio.test._

class JUnitComparableSpec extends zio.test.junit.JUnitRunnableSpec {

  override def spec: Spec[TestEnvironment with Scope, Any] =  suite("Another suite")(
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
