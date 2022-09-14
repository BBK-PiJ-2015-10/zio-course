package com.rockthejvm.part7streams

import zio._
import zio.stream._

import java.io.{IOException, InputStream}
//import zio.json._

object ZIOStreams extends ZIOAppDefault {

  //effects

  val aSucccess: ZIO[Any,Nothing,Int] = ZIO.succeed(42)

  // ZStream = "collection" (source) of 0 or more (maybe infinite) elements
  val aStream: ZStream[Any,Nothing,Int] = ZStream.fromIterable(1 to 10)

  val intStream: ZStream[Any,Nothing,Int]= ZStream(1,2,3,4,5,6,7,8)

  val stringStreams: ZStream[Any,Nothing,String] = intStream.map(_.toString)

  // sink = destination of the your elements
  // 4th => left over type
  // 5th => materialized value
  val sum: ZSink[Any,Nothing,Int,Nothing,Int] =
  ZSink.sum[Int]

  val take5: ZSink[Any,Nothing,Int,Int,Chunk[Int]] = ZSink.take(5)

  val take5Map: ZSink[Any,Nothing,Int,Int,Chunk[String]] = take5.map(chunck => chunck.map(_.toString))

  // leftovers                                          output      leftovers
  val take4LeftOvers: ZSink[Any,Nothing,Int,Nothing,(Chunk[String],Chunk[Int])] = take5Map.collectLeftover

  val take5Ignore: ZSink[Any,Nothing,Int,Nothing,Chunk[Int]] = take5.ignoreLeftover

  // contramap
  val take5String: ZSink[Any,Nothing,String,Int,Chunk[Int]] = take5.contramap(_.toInt)

  // ZStream[String] -> ZSink[Int].contramap(...)
  // ZStream[String].map(...) -> ZSink[Int]

  // Left on 16:56 https://www.youtube.com/watch?v=bp3nM6bfzJk
  val zio : ZIO[Any,Nothing,Int] = intStream.run(sum)

  // ZPipeline
  val businessLogic: ZPipeline[Any,Nothing,String,Int] =
    ZPipeline.map(_.toInt)


  val zio_v2: ZIO[Any,Nothing,Int] = stringStreams.via(businessLogic).run(sum)

  // many pipelines

  val filterLogic: ZPipeline[Any,Nothing,Int,Int] = ZPipeline.filter(_ % 2 == 0)

  val appLogic: ZPipeline[Any,Nothing,String,Int] =
    businessLogic >>> filterLogic

  val zio_v3: ZIO[Any,Nothing,Int] = stringStreams.via(appLogic).run(sum)

  val failStream: ZStream[Any,String,Int] = ZStream(1,2) ++ ZStream.fail("Something bad") ++ ZStream(4,5)

  class FakeInputStream[T <: Throwable](limit: Int, failedAt: Int, failWith: => T) extends InputStream {

    val data: Array[Byte] = "0123456789".getBytes()
    var counter: Int = 0
    var index: Int =0

    override def read(): Int = {
      if (counter == limit) -1
      else if (counter == failedAt) throw failWith
      else {
        val result = data(index)
        index = (index +1) % data.length
        counter +=1
        result
      }
    }
  }

  val nonFailingStream: ZStream[Any,IOException,String] = ZStream.fromInputStream(
    new FakeInputStream(12,99,new IOException("Something bad")),1
  ).map(byte => new String(Array(byte)))

  val failingStream: ZStream[Any,IOException,String] = ZStream.fromInputStream(
    new FakeInputStream(10,5,new IOException("Something bad")),1
  ).map(byte => new String(Array(byte)))

  val defectStream: ZStream[Any,IOException,String] = ZStream.fromInputStream(
    new FakeInputStream(10,5,new IllegalArgumentException("fucker")),5
  ).map(byte => new String(Array(byte)))

  // recovery
  val recoveryStream: ZStream[Any,Throwable,String] = ZStream("a","b","c")

  // or Else = chain a new Stream AT THE POINT of failure
  val recoveredEffect = failingStream.orElse(recoveryStream).run(sink)

  val recoveredWithEither: ZStream[Any, Throwable, Either[String, String]] = failingStream.orElseEither(recoveryStream)

  val recoveredWithEitherEffect = recoveredWithEither.run(ZSink.foreach(ZIO.succeed(_).debug))

  // catch
  val caughtErrors = failingStream.catchSome {
    case _ : IOException => recoveryStream
  }

  val caughtErrorsEffect = caughtErrors.run(sink).debug

  // catch SomeCourse, catchAll, catchAllCouse

  val errorContained: ZStream[Any, Nothing, Either[IOException, String]] = failingStream.either

  val errorContainedEffect = errorContained.run(ZSink.collectAll).debug

  val sink: ZSink[Any,Nothing,String,Nothing,String] =
    ZSink.collectAll[String].map(chunk => chunk.mkString("-"))



  override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] =  errorContainedEffect


}
