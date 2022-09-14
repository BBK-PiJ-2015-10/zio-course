package com.rockthejvm.part4coordination

import scala.collection.mutable.ListBuffer

object Testing  extends App {

  val list = List(1,2,3,4,5,6,7,8,9)

  private def chunkTakersMarkets(takersMarkets: List[Int], maxPartitions: Int) : List[List[Int]]  = {
    val groupedList = new ListBuffer[List[Int]]()
    if (takersMarkets.isEmpty || maxPartitions == 0) {
      return groupedList.toList
    }
    var pointer = 0
    val maxInvoiceChunkSize = 3
    while (pointer<takersMarkets.size){
      groupedList.append(takersMarkets.slice(pointer,pointer+maxInvoiceChunkSize))
      pointer += maxInvoiceChunkSize
    }
    groupedList.toList
  }



  //println(chunkTakersMarkets(list,3))

  println(list.slice(6,9))

}
