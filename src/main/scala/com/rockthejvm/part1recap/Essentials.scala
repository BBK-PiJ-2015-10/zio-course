package com.rockthejvm.part1recap

import java.util.concurrent.Executors
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Success, Try,Failure}

object Essentials {

  // values
  val aBoolean : Boolean = false

  // expressions are EVALUATED to a value
  val anIfExpression = if (2 > 3) "bigger" else "smaller"

  //instructions vs expression
  val theUnit = println("Hello Scala")

  //OOP
  class Animal
  class Cat extends Animal
  trait Carnivore {
    def eat(animal: Animal) : Unit
  }
  // many traits, but can only extend 1 class
 class Crocodile extends Animal with Carnivore {
    override def eat(animal: Animal): Unit = println("Delicous")
  }

  object MySingleton

  // companions
  object Carnivore

  class MyList[A]

  // method notation
  val three =  1 + 2
  val anotherThree = 1.+(2)

  // fp
  val incrementer : Int => Int  = x => x + 1

  val incremented = incrementer(45) // 46

  // map, flatMap, filter
  val processedList = List(1,2,3).map(incrementer) // List(2,3,4)

  // applies function and then concatenates
  val aLongerList =  List(1,2,3).flatMap(x => List(x,x + 1)) //List(1,2, 2,3, 3,4)

  val checkerBoard = List(1,2,3).flatMap(n => List('a','b','c').map(c => (n,c)))

  val anotherCheckerBoard = for {
    n <- List(1,2,3)
    c <- List('a','b','c')
  } yield (n,c)


  // options and try
  val anOption = Option(3)
  val aDoubledOption = anOption.map(_*2)

  val anAttempt = Try(42)

  val aModifiedAttemp : Try[Int] = anAttempt.map(_ + 10)

  // pattern matching
  val unKnown : Any = 45
  val ordinal = unKnown match {
    case 1 => "first"
    case 2 => "second"
    case _ => "unknown"
  }

  val optionExpression = anOption match {
    case Some(value) => s"The option is not empty $value"
    case None => "the option is empty"
  }




  // partial function
  val aPartialFunction : PartialFunction[Int,Int] = {
    case 1 => 43
    case 8 => 56
    case 100 => 99
  }

  trait HigherKindedType[F[_]]

  trait SequenceChecker[F[_]] {
    def isSequential : Boolean
  }

  val listChecker = new SequenceChecker[List] {
    override def isSequential: Boolean = true
  }

  // futures
  //implicit scala.concurrent.ExecutionContext.global


  def main(args: Array[String]): Unit = {

    implicit val ec = ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(8))

    val aFuture = Future {
      42
    }

    aFuture.onComplete{
      case Success(value) => println("The async meaning of life is")
      case Failure(exception) => println(s"Meaning value failed $exception")
    }

    aFuture.map(_ + 1)

   println("culon")



  }

}
