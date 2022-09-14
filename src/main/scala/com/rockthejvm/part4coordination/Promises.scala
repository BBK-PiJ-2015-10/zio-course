package com.rockthejvm.part4coordination

import com.rockthejvm.utilsScala2.DebugWrapper
import zio.{UIO, _}

object Promises extends ZIOAppDefault {

  val aPromise:ZIO[Any,Nothing,Promise[Throwable,Int]] = Promise.make[Throwable,Int]

  // await - block fiber until the promise has value
  val reader = aPromise.flatMap { promise =>
    promise.await
  }

  // succeed, fail, complete
  val writer = aPromise.flatMap { promise =>
  promise.succeed(42)
  }

  def demoPromise(): Task[Unit] = {
    // producer - consumer problem
    def consumer(promise: Promise[Throwable,Int]) = for {
      _  <- ZIO.succeed("[Consumer] waiting for result...").debugThread
      mol <- promise.await
      _   <- ZIO.succeed(s"[Consumer] I got the result $mol").debugThread
    } yield ()

    def producer(promise: Promise[Throwable,Int]) = for {
      -   <- ZIO.succeed(s"[Producer] crunching number").debugThread
      _   <- ZIO.sleep(3.seconds)
      _   <- ZIO.succeed(s"[Producer] complete")
      mol  <- ZIO.succeed(42)
      _   <- promise.succeed(mol)
    } yield ()

    for {
      promise <- Promise.make[Throwable,Int]
      -       <- consumer(promise).zipPar(producer(promise))
    } yield ()

  }

  /*
  - semantic lock on a fitber until you a get a signal from another fiber
  - inter-fiber communication
   */
  // simulate downloading from multiple parts
  val fileParts = List("I ","love S","cala"," with pure FP an","d ZIO! <EOF>")

  def downloadFileWithRef() : UIO[Unit] = {
    def downloadFile(contentRef: Ref[String]): UIO[Unit] =
      ZIO.collectAllDiscard(
        fileParts.map { part =>
          ZIO.succeed(s"got part '$part'").debugThread *> ZIO.sleep(1.second) *> contentRef.update(_ + part)
        }
      )

    def notifyFileComplete(contentRef: Ref[String]): UIO[Unit] = for {
      file <- contentRef.get
      _    <- if (file.endsWith("<EOF>")) ZIO.succeed(s"File download complete").debugThread
      else ZIO.succeed("downloading ...").debugThread *> ZIO.sleep(500.millis) *> notifyFileComplete(contentRef)
    } yield ()

    for {
      contentRef <- Ref.make("")
      -          <- downloadFile(contentRef) zipPar notifyFileComplete(contentRef)
    } yield ()

  }

  def downloadFileWithRefPromise() : Task[Unit] = {

    def downloadFile(content: Ref[String],promise: Promise[Throwable,String]) = {
      ZIO.collectAllDiscard(
        fileParts.map { part =>
          for {
            _  <- ZIO.succeed(s"got part '$part'").debugThread
            _  <- ZIO.sleep(1.second)
            file <- content.updateAndGet(_ + part)
            _    <- if (file.endsWith("<EOF>")) promise.succeed(file) else ZIO.unit
          } yield ()
        }
      )
    }

    def notifyFileComplete(promise: Promise[Throwable,String]) : Task[Unit] = {
      for {
        -    <- ZIO.succeed("downloading...").debugThread
        file <- promise.await
        _    <- ZIO.succeed(s"file downloaded complete: $file").debugThread
      } yield()
    }

    for {
      contentRef <- Ref.make("")
      promise <- Promise.make[Throwable,String]
      -       <- downloadFile(contentRef,promise).zipPar(notifyFileComplete(promise))
    } yield ()

    //ZIO.unit

  }

  /**
   *  Simulated egg boiler
   *  - one increment a counter every 1s. Atomic ref
   *  - one waits for the counter to become 10, after which it will ring a bell
   *
   */
    def eggBoiler(): Task[Unit] = {

      def incrementer(counter: Ref[Int], promise: Promise[Throwable,Int]) : Task[Unit] = {
        for {
          _   <- ZIO.succeed("incrementing").debugThread
          -   <- ZIO.sleep(1.second)
          newCount   <- counter.updateAndGet(_+ 1)
          _   <- ZIO.succeed(s"incremented counter to $newCount").debugThread
          -   <- if (newCount == 10) promise.succeed(newCount) else incrementer(counter, promise)
        } yield ()
      }

      def reporter(promise: Promise[Throwable,Int]) : Task[Unit] = {
        for {
          _   <- ZIO.succeed("reporting").debugThread
          newCount   <- promise.await
          _   <- ZIO.succeed(s"reported count of $newCount").debugThread
        } yield ()
      }

      for {
        counter <- Ref.make(0)
        promise <- Promise.make[Throwable,Int]
        -       <- incrementer(counter,promise).zipPar(reporter(promise))
      } yield ()

    }

  /*
  2. Write a race pair
    - Use a promise which can hold on Either[exit for A, exit for B]
    - start a fiber for each ZIO.
    - on completion (with any status), each ZIO needs to complete that Promise (hint: use a finalizer)
    - waiting on that Promise's value can be interrupted
    - if the whole trace is interrupted, interrupt the running fibers
   */
  //def racePair[R,E,A,B](zioa: => ZIO[R,E,A],zio: ZIO[R,E,B]) :
  //ZIO[R,Nothing,Either[(Exit[E,A],Fiber[E,B]),(Fiber[E,A],Exit[E,B])]] = ???


  def racePair[R,E,A,B](zioa: => ZIO[R,E,A],ziob: ZIO[R,E,B]) : ZIO[R,Nothing,Either[(Exit[E,A],Fiber[E,B]),(Fiber[E,A],Exit[E,B])]] =
    ZIO.uninterruptibleMask { restore =>
      for {
        promise <- Promise.make[Nothing,Either[Exit[E,A],Exit[E,B]]]
        fiba   <- zioa.onExit(exita => promise.succeed(Left(exita))).fork
        fibb   <- ziob.onExit(exitb => promise.succeed(Right(exitb))).fork

        result <- restore(promise.await).onInterrupt{
          for {
            interruptA <- fiba.interrupt.fork
            interruptB <- fibb.interrupt.fork
            -          <- interruptA.join
            -          <- interruptB.join
          } yield ()
        }

      } yield result match {
        case Left(exita) => Left((exita,fibb))
        case Right(exitb) => Right((fiba,exitb))
      }

    }

  val demoRacePair = {
    val zioA = ZIO.sleep(3.second).as(1).onInterrupt(ZIO.succeed("first interrupted").debugThread)
    val zioB = ZIO.sleep(2.second).as(2).onInterrupt(ZIO.succeed("second interrupted").debugThread)
    val pair = racePair(zioA,zioB)

    pair.flatMap {
      case Left((exita,fibb)) => fibb.interrupt *> ZIO.succeed("first won").debugThread *> ZIO.succeed(exita).debugThread
      case Right((fiba,exitb)) => fiba.interrupt *> ZIO.succeed("second won").debugThread *> ZIO.succeed(exitb).debugThread
    }
  }




  override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] = demoRacePair
}
