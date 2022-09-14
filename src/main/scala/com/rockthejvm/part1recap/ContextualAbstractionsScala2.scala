package com.rockthejvm.part1recap

object ContextualAbstractionsScala2 {

  case class Person(name: String) {
    def greet(): String = s"Hi, my name is $name"
  }

  implicit class ImpersonableString(name: String) {
    def greet() : String = Person(name).greet()
  }

  // extension method
  val greeting = "Peter".greet()   // new ImpersonableString("Peter").greet()

  // example: scala.concurrent.duration

  import scala.concurrent.duration._
  val oneSecond = 1.second

  // implicit arguments and values

  def increment(x: Int)(implicit amount: Int) = x + amount

  implicit val defaultAmount: Int = 10

  val result = increment(2)  //implicit argument 10 passed by the compiler

  def multiple(x: Int)(implicit factor: Int) = x * factor
  multiple(10)

  // more complex example

  trait JSONSerializer[T] {
    def toJson(value: T): String
  }

  def convert2Json[T](value: T)(implicit serializer: JSONSerializer[T]): String  = serializer.toJson(value)

  implicit val personSerializer : JSONSerializer[Person] = new JSONSerializer[Person] {
    override def toJson(value: Person): String = "{\"name\":\"" +value.name +"\"}"
  }

  val davidsToJson = convert2Json(Person("David"))

  // implicit defs

  implicit def createListSerializer[T](implicit serializer: JSONSerializer[T]): JSONSerializer[List[T]] = new JSONSerializer[List[T]] {
    override def toJson(value: List[T]): String = s"[${value.map(serializer.toJson).mkString(",")}]"
  }

  val personJson = convert2Json(List(Person("Alice"),Person("Bob")))

  //implicit conversions (not recommended)
  case class Cat(name: String) {
    def meaow(): String = s"$name is meqouwing"
  }

  implicit def string2Cat(name: String): Cat = Cat(name)

  val aCat: Cat = "Garfield"

  val garfieldMeawing = "Garfield".meaow()


  def main(array: Array[String]) : Unit = {

   println(davidsToJson)

   println(personJson)



  }

}
