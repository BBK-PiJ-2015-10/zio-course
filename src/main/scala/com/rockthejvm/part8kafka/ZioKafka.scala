package com.rockthejvm.part8kafka

import org.apache.kafka.clients.producer._
import zio._
import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder, JsonDecoder, JsonEncoder}
import zio.kafka.consumer._
import zio.kafka.serde.Serde
import zio.json._
import zio.stream.{ZSink, ZStream}

// extends zio.App

// source: https://blog.rockthejvm.com/zio-kafka/

object ZioKafka extends ZIOAppDefault {

  val consumerSettings = ConsumerSettings(List("localhost:9092"))
    .withGroupId("updates-consumer2") // settings for the kakfa consumer

  val managedConsumer: ZIO[Scope, Throwable, Consumer] = Consumer.make(consumerSettings)

  val consumer: ZLayer[Scope, Throwable, Consumer] = ZLayer.fromZIO(managedConsumer)

  // stream of strings, read from the kafka topic
  val footballMatchesStream = Consumer.subscribeAnd(Subscription.topics("updates")).plainStream(
    Serde.string,Serde.string)

  case class MatchPlayer(name: String, score: Int){
    override def toString: String = s"$name : $score"
  }

  object MatchPlayer {
    implicit val encoder: JsonEncoder[MatchPlayer] = DeriveJsonEncoder.gen[MatchPlayer]
    implicit val decoder: JsonDecoder[MatchPlayer] = DeriveJsonDecoder.gen[MatchPlayer]
  }

  case class Match(players: Array[MatchPlayer]) {
    def score: String = s"${players(0)} - ${players(1)} "
  }
  object Match {
    implicit val encoder: JsonEncoder[Match] = DeriveJsonEncoder.gen[Match]
    implicit val decoder: JsonDecoder[Match] = DeriveJsonDecoder.gen[Match]
  }

  // json Strings -> Kafka -> jsons -> Match instances

 val matchSerde: Serde[Any,Match] = Serde.string.inmapM {
   string =>
     // deserializer
     ZIO.fromEither(string.fromJson[Match].left.map(errorMessage => new RuntimeException(errorMessage)))
 } { myMatch =>
     // serializer
     ZIO.from(myMatch.toJson)
 }

  val matchesStream = Consumer.subscribeAnd(Subscription.topics("updates"))
    .plainStream(Serde.string,matchSerde)

  val matchesPrintableStream: ZStream[Any with Consumer, Throwable, OffsetBatch] =
    matchesStream // stream of matches instances
      .map(cr => (cr.value.score,cr.offset))  // stream of tuples
      .tap {
        case (score,_) => Console.printLine(s"| $score |")
      }
      .map(_._2) // stream of offsets
      .aggregateAsync(Consumer.offsetBatches) // stream of offsets


  val streamEffect: ZIO[Any with Consumer, Throwable, Unit] = matchesPrintableStream.run(ZSink.foreach(record => record.commit))


  override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] =
    streamEffect.provideLayer(consumer).exit

}
