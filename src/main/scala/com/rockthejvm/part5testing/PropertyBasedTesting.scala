package com.rockthejvm.part5testing

import com.rockthejvm.utilsScala2.DebugWrapper
import zio._
import zio.test._

object PropertyBasedTesting extends ZIOSpecDefault {

  // "proofs"
  // for all, x,y,z
  // shrinking
  override def spec: Spec[TestEnvironment with Scope, Any] = test("propery based testing basics"){
    check(Gen.int,Gen.int,Gen.int) { (x,y,z) =>
      assertTrue(((x+y)+z) == (x - (y + z)))
    }
  }

  /*
  GEN[R,A]
  R => Environment
  A => Value of generator
   */
  val intGenerators = Gen.int
  val charGenerator = Gen.char
  val stringGenerator = Gen.string
  val cappedLengthStringGenerator = Gen.stringN(10)(Gen.alphaNumericChar)
  val constGenerator = Gen.const("scala")
  val valuesGenerator = Gen.elements(1,2,3,4,5)
  val valuesIterable = Gen.fromIterable(1 to 1000)
  val uniformDoubles = Gen.uniform // select doubles between 0 to 1

  //product collections
  val listGenerator = Gen.listOf(Gen.string)
  val finiteSetGenerator = Gen.setOfN(10)(Gen.int)

  // options, either
  val optionGenerator = Gen.option(Gen.int)
  val eitherGenerator = Gen.either(Gen.string,Gen.int)

  //combinators
  val zippedGenerator = Gen.int.zip(Gen.string) // produces (Int,String)
  val filteredGenerator = intGenerators.filter(_ % 3 == 0)
  val mappedGenerator = intGenerators.map(n => (1 to n).map(_ => 'a').mkString)
  val flatMappedGenerator = filteredGenerator.flatMap(l => Gen.stringN(l)(Gen.alphaNumericChar))

  // for-comprehension
  val uuidGenerator = for {
    part1 <- Gen.stringN(8)(Gen.alphaNumericChar)
    part2 <- Gen.stringN(4)(Gen.alphaNumericChar)
    part3 <- Gen.stringN(4)(Gen.alphaNumericChar)
    part4 <- Gen.stringN(12)(Gen.alphaNumericChar)
  } yield s"$part1-$part2-$part3-$part4"

  //general
  val randomGenerator = Gen.fromRandom(random => random.nextUUID)
  val effectGenerator = Gen.fromZIO(ZIO.succeed(42))
  val generalGenerator = Gen.unfoldGen(0)(i => Gen.const(i +1).zip(Gen.stringN(i)(Gen.alphaNumericChar)))
  // lists of Strings with the property that every string will have an increasing length

}

object GeneratorPlayground extends ZIOAppDefault {

  override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] = {
    val generalGenerator  = Gen.unfoldGen(0)(i => Gen.const(i +1).zip(Gen.stringN(i)(Gen.alphaNumericChar)))
    val generatedListZio = generalGenerator.runCollectN(100)
    val generatedListZio_v2 = generatedListZio.provideLayer(Sized.live(50))
    generatedListZio_v2.debugThread
    //generatedListZio.provideLayer(Sized.default)
  }


}