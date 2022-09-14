package com.rockthejvm.part7streams

import zio._
import zio.stream._
import zio.json._

import java.nio.charset.CharacterCodingException
import scala.util.matching.Regex

object ZIOStreamsExample extends ZIOAppDefault {


  // Even pretend blog posts need a [generic](/tags/generic) intro

  val post1: String = "hello-word.md"
  val post1_content: Array[Byte] =
    """---
      |title: "Hello World"
      |tags: []
      |---
      |======
      |
      |## Generic Heading
      |
      |Even pretend blog posts need a #generic intro.
      |""".stripMargin.getBytes

  val post2: String = "scala-3-extensions.md"
  val post2_content: Array[Byte] =
    """---
      |title: "Scala 3 for You and Me"
      |tags: []
      |---
      |======
      |
      |## Cool Heading
      |
      |This is a post about #Scala and their re-work of #implicits via thing like #extensions.
      |""".stripMargin.getBytes

  val post3: String = "zio-streams.md"
  val post3_content: Array[Byte] =
    """---
      |title: "ZIO Streams: An Introduction"
      |tags: []
      |---
      |======
      |
      |## Some Heading
      |
      |This is a post about #Scala and #ZIO #ZStreams!
""".stripMargin.getBytes

  val fileMap: Map[String, Array[Byte]] = Map(
    post1 -> post1_content,
    post2 -> post2_content,
    post3 -> post3_content
  )

  val hashFilter: String => Boolean = str => str.startsWith("#") && (str.count(_ == '#') == 1) && str.length >= 2

  val parseHash: ZPipeline[Any,Nothing,String,String] = ZPipeline.filter(hashFilter)

  val punctRegex: Regex = """\p{Punct}""".r

  val removePunctuation: ZPipeline[Any,Nothing,String,String] = ZPipeline.map(str => punctRegex.replaceAllIn(str,""))

  val lowerCase: ZPipeline[Any,Nothing,String,String] = ZPipeline.map(_.toLowerCase())

  val collectTagsPipeline: ZPipeline[Any, CharacterCodingException, Byte, String] =
    ZPipeline.utf8Decode >>>
      ZPipeline.splitLines >>>
      ZPipeline.splitOn(" ") >>>
      parseHash >>>
      removePunctuation >>>
      lowerCase

  val addTags: Set[String] => ZPipeline[Any,Nothing,String,String] = tags =>
    ZPipeline.map(content => content.replace("tags: []",s"tags: [${tags.mkString(", ")}]"))



  val addLinks: ZPipeline[Any,Nothing,String,String] =
    ZPipeline.map { line =>
      line.split(" ").map { word =>
        if (hashFilter(word)) {
          s"[$word]/(/tags/${punctRegex.replaceAllIn(word.toLowerCase,"")})"
        } else {
          word
        }
      }.mkString(" ")
    }

  val addNewline : ZPipeline[Any,Nothing,String,String] = ZPipeline.map(line => line +"\n")

  val regeneratePost: Set[String] => ZPipeline[Any,CharacterCodingException,Byte,Byte] = tags =>
    ZPipeline.utf8Decode >>>
      ZPipeline.splitLines >>>
      addTags(tags) >>>
      addLinks >>>
      addNewline >>>
      ZPipeline.utf8Encode


  def writeFile(dirPath: String, fileName: String): ZSink[Any,Throwable,Byte,Byte,Long] =
    ZSink.fromFileName(dirPath + "/" +fileName)

  val collectTags: ZSink[Any,Nothing,String,Nothing,Set[String]] = ZSink.collectAllToSet

  def autoTag(fileName: String, contents:Array[Byte]): ZIO[Any, Throwable, (String, Set[String])] = for {
    tags <- ZStream.fromIterable(contents)
      .via(collectTagsPipeline)
      .run(collectTags)
    _  <- Console.printLine(s"Generating file $fileName")
    _  <- ZStream.fromIterable(contents)
      .via(regeneratePost(tags))
      .run(writeFile("src/main/resources/data/zio-streams",fileName))
  } yield (fileName,tags)


  val autoTagAll: ZIO[Any, Throwable, Map[String, Set[String]]] = ZIO.foreach(fileMap) {
    case (filename, contents) => autoTag(filename,contents)
  }

  def createTagIndex(tagMap: Map[String,Set[String]]) = {
    val searchMap = tagMap
      .values
      .toSet
      .flatten
      .map(tag => tag -> tagMap.filter(_._2.contains(tag)).keys.toSet)
      .toMap

    ZStream.fromIterable(searchMap.toJsonPretty.getBytes)
      .run(ZSink.fromFileName("src/main/resources/data/zio-streams/search.json"))

  }


  val parseProgram = for {
    tagMap  <- autoTagAll
    _       <- Console.printLine(s"Generating index file search.json")
    _       <- createTagIndex(tagMap)
  } yield ()



  override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] = parseProgram
}
