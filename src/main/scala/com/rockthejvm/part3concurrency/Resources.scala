package com.rockthejvm.part3concurrency

import com.rockthejvm.utilsScala2.DebugWrapper
import zio._

import java.io.File
import java.util.Scanner

object Resources extends ZIOAppDefault {

  // finalizers

  def unsafeMethod(): Int = throw new Exception("Not an int here for you")

  val anAttempt = ZIO.attempt(unsafeMethod())

  // finalizers
  val attemptWithFinalizer = anAttempt.ensuring(ZIO.succeed("finalizer").debugThread)

  // multiple finalizer
  val attemptWith2Finalizer = attemptWithFinalizer.ensuring(ZIO.succeed("another finalizer!").debugThread)
  // Other examples of finalizers. .onInterrupt, .onError, .onDone, .onExit

  // resource lifecycle

  class Connection(url: String) {
    def open() = ZIO.succeed(s"opening connection to $url ...").debugThread

    def close() = ZIO.succeed(s"Closing connection to $url ...").debugThread
  }

  object Connection {
    def create(url: String) = ZIO.succeed(new Connection(url))
  }

  val fetchUrl = for {
    conn <- Connection.create("rockthejvm.com")
    fib <- (conn.open *> ZIO.sleep(300.seconds)).fork
    _ <- ZIO.sleep(1.second) *> ZIO.succeed("interrupting").debugThread *> fib.interrupt
    _ <- fib.join
  } yield () // resource leak

  val correctFetchUrl = for {
    conn <- Connection.create("rockthejvm.com")
    fib <- (conn.open *> ZIO.sleep(300.seconds)).ensuring(conn.close()).fork
    _ <- ZIO.sleep(1.second) *> ZIO.succeed("interrupting").debugThread *> fib.interrupt
    _ <- fib.join
  } yield () // no longer resource leak

  //acquireRelease
  // properties
  // acquiring can not be interrupted
  // all finalizers are guarantteed to run

  val cleanConnection: ZIO[Any with Scope, Nothing, Connection] = ZIO.acquireRelease(Connection.create("rockTheJvm"))(_.close())

  val fetchWithResource: ZIO[Any with Scope, Nothing, Unit] = for {
    conn <- cleanConnection
    fib <- (conn.open *> ZIO.sleep(300.seconds)).fork
    _ <- ZIO.sleep(1.second) *> ZIO.succeed("interrupting").debugThread *> fib.interrupt
    _ <- fib.join
  } yield ()

  val fetchWithScopedResource: ZIO[Any, Nothing, Unit] = ZIO.scoped(fetchWithResource)

  // acquireReleaseWith  => acquire, release, usage
  val cleanConnection_v2: ZIO[Any, Nothing, Unit] = ZIO.acquireReleaseWith(Connection.create("rockthejvm.com"))(_.close())(
    conn => conn.open *> ZIO.sleep(300.seconds)
  )

  val fetchWithResources_v2: ZIO[Any, Nothing, Unit] = for {
    fib <- cleanConnection_v2.fork
    _ <- ZIO.sleep(1.second) *> ZIO.succeed("interrupting").debugThread *> fib.interrupt
    _ <- fib.join
  } yield ()

  /*
  Exercises
  1 . Use the acquireRelease to open a file, print all lines, (one every 100 millis), then close the file
   */


  // scanner.hasNext scanner.nextLine
  def openFileScanner(path: String): UIO[Scanner] =
    ZIO.succeed(new Scanner(new File(path)))

  def readLineByLine(scanner: Scanner): UIO[Unit] =
    if (scanner.hasNext()) ZIO.succeed(scanner.nextLine()).debugThread *> ZIO.sleep(100.millis) *> readLineByLine(scanner)
    else
      ZIO.unit

  def acquireOpenFile(path: String): UIO[Unit] =
    ZIO.succeed(s"openning file at $path").debugThread *>
      ZIO.acquireReleaseWith(
        openFileScanner(path)
      )(
        scanner => ZIO.succeed(s"closing file at $path").debugThread *> ZIO.succeed(scanner.close())
      )(readLineByLine)

//  def test(path: String) = ZIO.acquireReleaseWith(
//    openFileScanner(path))(_.close())(scanner =>
//    for {
//      hasNext  <- ZIO.from(scanner.hasNext)
//      _        <- ZIO.from(println(scanner.next())).when(hasNext)
//    } yield()
//  )

  val testInterruptFileDisplay = for {
    fib <- acquireOpenFile("src/main/scala/com/rockthejvm/part3concurrency/Resources.scala").fork
    _   <- ZIO.sleep(2.seconds) *> fib.interrupt
  } yield ()

  //acquireRelease, acquireReleaseWith
  def connFromConfig(path: String): UIO[Unit] =
    ZIO.acquireReleaseWith(openFileScanner(path))(scanner => ZIO.succeed("closingfile").debugThread *> ZIO.succeed(scanner.close())){
      scanner => ZIO.acquireReleaseWith(Connection.create(scanner.nextLine()))(_.close()){
        conn => conn.open() *> ZIO.never
      }
    }

  //nested resources
  def connectionFromConfig_v2(path: String): UIO[Unit] = ZIO.scoped { for {
    scanner <- ZIO.acquireRelease(openFileScanner(path))(scanner => ZIO.succeed("closingfile").debugThread *> ZIO.succeed(scanner.close()))
    conn     <- ZIO.acquireRelease(Connection.create(scanner.nextLine()))(_.close())
    _        <- conn.open() *> ZIO.never
  } yield()}


  override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] =
    connectionFromConfig_v2("src/main/resources/connections.conf")
}
