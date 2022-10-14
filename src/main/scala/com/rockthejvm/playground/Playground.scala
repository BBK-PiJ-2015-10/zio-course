package com.rockthejvm.playground

import java.util.PriorityQueue

object Playground {
  def main(args: Array[String]): Unit = {

    val scalaPriorityQueue = collection.mutable.PriorityQueue(1,2,3,4)

    val javaPriorityQueue : PriorityQueue[Int] = new PriorityQueue()
    javaPriorityQueue.offer(1)
    javaPriorityQueue.offer(2)
    javaPriorityQueue.offer(3)
    javaPriorityQueue.offer(4)

    println(scalaPriorityQueue.head)

    println(javaPriorityQueue.poll())

    println("ZIO is set up, let's go!")
  }
}
