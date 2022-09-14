package com.rockthejvm.part8kafka

import org.apache.kafka.clients.producer.{ProducerRecord, RecordMetadata}
import zio._
import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder, JsonDecoder, JsonEncoder}
import zio.kafka.consumer._
import zio.kafka.producer._
import zio.kafka.serde.Serde
import zio.json._
import zio.stream.{ZSink, ZStream}

// source: https://blog.rockthejvm.com/zio-kafka/

/**
 * To manipulate:
 *  - docker-compose up -d
 *  - docker exec -it broker bash
 *  - kafka-topics --bootstrap-server localhost:9092 --topic updates --create
 *  kafka-console-producer --topic updates --broker-list localhost:9092 --property parse.key=true --property key.separator=,
 *  update-1,{"players":[{"name":"ITA","score":0},{"name":"England","score":2}]}
 */


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

object ZIOKafkaProducer extends ZIOAppDefault {

  import ZioKafka._

  val producerSettings: ProducerSettings = ProducerSettings.apply(List("localhost:9092"))

  val producerResource: ZIO[Scope, Throwable, Producer] = Producer.make(producerSettings)
  val producer: ZLayer[Scope, Throwable, Producer] =  ZLayer.fromZIO(producerResource)

  val finalScore: Match = Match(Array(
    MatchPlayer("ITA",6),
    MatchPlayer("ENG",6)
  ))

  val record: ProducerRecord[String, Match] = new ProducerRecord[String,Match]("updates","updates-6",finalScore)

  val producerEffect: RIO[Any with Producer, RecordMetadata] = Producer.produce(record,Serde.string,matchSerde)


  override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] =
    producerEffect.provideLayer(producer).exit

}
