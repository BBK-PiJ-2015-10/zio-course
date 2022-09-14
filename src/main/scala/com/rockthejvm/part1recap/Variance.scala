package com.rockthejvm.part1recap

import com.rockthejvm.part1recap.Variance.Animal

import java.util

object Variance {

  class Animal
  class Dog(name: String) extends Animal
  class Cat(name: String) extends Animal
  // Variance question

  // Yes, covariant
  val lassie = new Dog("Lassie")
  val hachi = new Dog("Lacchi")
  val laika = new Dog("Laika")
  val anAnimal : Animal = lassie
  val someAnimal: List[Animal] = List(lassie,hachi,laika)

  // covariant
  class MyList[+A]
  val myAnimalList: MyList[Animal] = new MyList[Dog]

  //invariant
  trait SemiGroup[A] {
    def combine(x: A, y: A): A
  }

  // all generics in java
  val aJavaList : java.util.ArrayList[Animal] = new util.ArrayList[Animal]()
  aJavaList.add(laika)

  //HELL No - Contravariant

  trait Vet[-A] {
    def heal(animal: A): Boolean
  }

  // Vet[Animal]
  // Dog <: Animal , then Vet[Dog] >: Vet[Animal]
  val myVet: Vet[Dog] = new Vet[Animal] {
    override def heal(animal: Animal): Boolean = {
      println("Here you go, you are good now")
      true
    }
  }

  /*
   * - If the type PRODUCES or RETRIEVES values of type A (e.g. List), then the type should be COVARIANT
   * - If the type CONSUMES or ACTS on value of type A (e.g. a vet), then the type should be CONTRAVARIANT
   * - Otherwise, INVARIANT
   */

  /**
   *  Variance position
   *
   */

//  /**
//   *  class Vet2[-A](val favoriteAnimal[A]) <-- the type of val fields are in COVARIANT positions
//
//      val garfield = new Cat
//      val theVet : Vet2[Animal] = new Vet2[Animal](garfield)
//      val dogVet:  Vet2[Dog]  = theVet
//      val favAnimal: Dog = dogVet.favoriteAnimal  // must be a dog - type conflict
//
//     var fields are also in COVARIANT position
//
//    class MutableContainer[+A](var contents: A)
//    val containerAnimal : MutableContainer[Animal] = new MutableContainer[Dog](new Dog)
//
//    containerAnimal.contents = new Cat   // type conflict
//
//    var are only compatible with invariant types
//
//    //type of methods arguments are in CONTRAVARIANT position
//
// =   *
//   */

  // solution widen the type
  abstract class MyList2[+A] {
    def add[B >: A](element: B): MyList2[B]
    //def add(element: A): MyList2[A]
  }

//  val animals: MyList2[Animal] = new MyList2[Cat]
//  val biggerListOfAnimal: MyList2[Animal] = animals.add(Dog("ale"))

  abstract class Vet2[-A] {
    def rescueAnimal[B <: A]() : B
  }

  /**
   *
   * val vet: Vet2[Animal] = new Vet2[Animal] {
   *  def rescueAnimal(): Animal = new Cat
   * }
   *  val lassieVet: Vet2[Dog] = vet
   *  val rescueDog: Dog = lassieVet.rescueAnimal // must return a dog, but returns a cat
   *
   */



  // method return types are in Covariant position




  def main(args: Array[String]): Unit = {




  }

}

