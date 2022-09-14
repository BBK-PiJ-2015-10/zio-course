package com.rockthejvm.part4coordination

import com.rockthejvm.utilsScala2.DebugWrapper
import zio._
import zio.stm._


object TransactionalEffects extends ZIOAppDefault {

  // STM = "atomic effect"
  val anSTM: ZSTM[Any,Nothing,Int] = STM.succeed(42)
  val aFailedSTM = STM.fail("something bad")
  val anAttempSTM : ZSTM[Any,Throwable,Int] = STM.attempt(42 / 0)
  // map, flatMap, for comprehensions

  // type aliases
  val ustm: USTM[Int] = STM.succeed(2)
  val anSTM_V2: STM[Nothing,Int] = STM.succeed(42)

  // STM vs ZIO
  // compose STMs to obtain other STMs
  // the evaluation FULLLY ATOMIC
  // "COMMIT"
  val anAttempEffect : ZIO[Any,Throwable,Int] = anAttempSTM.commit

  val safeUpdate: PartialFunction[Long,Long] = {
    case x if x >= 0 => x + 2
  }

  def safeUpdate3(amount: Long): PartialFunction[Long,Long] = {
    case senderBalance: Long => (senderBalance - amount) match {
      case x if x > 0 => (senderBalance - amount)
      case _   => throw new RuntimeException("fucker")
    }

  }



  // example
  def transferMoney(sender: Ref[Long], receiver: Ref[Long],amount: Long): ZIO[Any, String, Long] = {
    for {
      senderBalance <- sender.get
      _             <- ZIO.fail("Transfer failed: Insufficient funds").when(senderBalance < amount)
      _             <- if (senderBalance < amount) ZIO.fail("Transfer failed: Insufficient funds") else ZIO.unit
      _             <- sender.update(_ - amount)
      _             <- receiver.update(_ + amount)
      newBalance    <- sender.get
    } yield newBalance
  }

  def transferMoneyTransactional(sender: TRef[Long], receiver: TRef[Long],amount: Long): STM[String, Long] = {
    for {
      senderBalance <- sender.get
      _             <- if (senderBalance < amount) STM.fail("Transfer failed: Insufficient funds") else STM.unit
      _             <- sender.update(_ - amount)
      _             <- receiver.update(_ + amount)
      newBalance    <- sender.get
    } yield newBalance
  }

  def exploitBuggyBank() = for {
    sender <- Ref.make(1000L)
    receiver <- Ref.make(0L)
    fib1     <- transferMoney(sender,receiver,1000).fork
    fib2     <- transferMoney(sender,receiver,1000).fork
    _        <- (fib1 zip fib2).join
    -        <- receiver.get.debugThread
  } yield ()

  def cannotExploitBuggyBank() = for {
    sender <- TRef.make(1000L).commit
    receiver <- TRef.make(0L).commit
    fib1     <- transferMoneyTransactional(sender,receiver,1000).commit.fork
    fib2     <- transferMoneyTransactional(sender,receiver,1000).commit.fork
    _        <- (fib1 zip fib2).join
    -        <- receiver.get.commit.debugThread
  } yield ()

  def loop(i: Int): ZIO[Any,Any,Unit] =
    if (i > 1000) ZIO.unit else exploitBuggyBank().ignore *> loop(i +1)

  def loop2(i: Int): ZIO[Any,Any,Unit] =
    if (i > 1000) ZIO.unit else cannotExploitBuggyBank().ignore *> loop2(i +1)

  //
  /*
  STM data structures
  // atomic variable: TRef
  // same API: get, update, modify, set
   */
  val aVariable: USTM[TRef[Int]] = TRef.make(42)

  //TArray
  val specifiedValuesTArray: USTM[TArray[Int]] = TArray.make(1,2,3)
  val iterableArray: USTM[TArray[Int]] = TArray.fromIterable(List(1,2,3,3))
  // get/apply
  val TArrayGetElem: USTM[Int] = for {
    tArray <- iterableArray
    elem   <- tArray(2)
  } yield elem

  //update
  val TArrayUpdateElem: USTM[TArray[Int]] = for {
    tArray <- iterableArray
    _      <- tArray.update(1,elem => elem + 10)
  } yield tArray

  //transform
  val transformedArray: USTM[TArray[Int]] =  for {
    tArray <- iterableArray
    _      <- tArray.transform(_ * 10)
  } yield tArray

  // fold/foldSTM,forEach

  //Tset

  val specificValuesTSet : USTM[TSet[Int]] = TSet.make(1,2,3,4,5,1,2,3)

  val tSetContainsElem: USTM[Boolean] = for {
    tSet <- specificValuesTSet
    res  <- tSet.contains(3)
  } yield res

  //put
  val putElem: USTM[TSet[Int]] = for {
    tSet <-  specificValuesTSet
    _    <-   tSet.put(7)
  } yield tSet

  //remove
  val delElem: USTM[TSet[Int]] = for {
    tSet <-  specificValuesTSet
    _    <- tSet.delete(1)
  } yield tSet

  //Tmap

  val aTMapEffect: USTM[TMap[String,Int]] = TMap.make(("Dane",123),("Alice",234),("Dog",456))

  val putElemTMap: USTM[TMap[String,Int]] = for {
    tMap <- aTMapEffect
    _    <- tMap.put("Master Yoga",456)
  } yield tMap

  val getElemTMap: USTM[Option[Int]] = for {
    tMap <- aTMapEffect
    elem <- tMap.get("Dane")
  } yield elem

  //TQueue

  val tQueueBounded: USTM[TQueue[Int]] = TQueue.bounded[Int](40)

//  val demoOffer: USTM[TQueue[Int]] = for {
//    tQueue  <- demoOffer
//    _       <- tQueue.offerAll(List(1,2,3,4,5,6))
//  } yield tQueue

//  val demoTakeAll: USTM[Chunk[Int]] = for {
//    tQueue   <- demoOffer
//    result        <- tQueue.takeAll
//  } yield result

  //TPqueue
  val maxQueue: USTM[TPriorityQueue[Int]] = TPriorityQueue.make(3,4,1,2,5)

  /*
  Concurrent coordination
   */

  //TRef
  //TPromise
  val tPromiseEffect : USTM[TPromise[String,Int]] = TPromise.make[String,Int]
  // await
  // poll to see if it has been completed or not and not block as in await
  // succeed/fail/complete

  val tPromiseAwait: STM[String,Int] = for {
    p  <- tPromiseEffect
    res  <- p.await
  } yield res

  val demoSucceed: USTM[Unit] =  for {
    p  <- tPromiseEffect
    _  <- p.succeed(45)
  } yield()

  //TSemaphore
  val tSemaphoreEffect : USTM[TSemaphore] = TSemaphore.make(10)
  //acquire + acquireN
  val semaphareAcq: USTM[Unit] = for {
    t <- tSemaphoreEffect
    // The below will semantically block
    _ <- t.acquire
  } yield ()
  // release + releaseN
  val TSemaphoreRelease: USTM[Unit] = for {
    t <- tSemaphoreEffect
    _ <- t.release
  } yield ()

  // withPermit
  val semWithPermit: UIO[Int] = tSemaphoreEffect.commit.flatMap{ sem =>
    sem.withPermit {
      ZIO.succeed(42)
    }
  }

  //TReentranct lock - can acquire the same lock multimple times without deadlock
  // readers-writers problem
  // has tow locks: read lock (lower priority) and write lock (higher priority)

  val reentrantLockEffect = TReentrantLock.make

//  val demoReentrantLock = for {
//    lock <- reentrantLockEffect
//    _    <- lock.acquireRead //acquire the read lock
//    -    <- STM.succeed(100) // critical section, only those that acquire the read lock can access
//    rl    <- lock.readLock // status of the lock, whether is read-locked, true in this case
//    wl    <- lock.writeLock
//  } yield ()

  def demoReadersAndWriters()  = {
    def read(i: Int, lock: TReentrantLock): UIO[Unit] = for {
      _   <- lock.acquireRead.commit
      // critical region
      _   <- ZIO.succeed(s"[Task $i] taken the read lock, reading...").debugThread
      time <- Random.nextIntBounded(1000)
      _    <- ZIO.sleep(time.millis)
      res  <- Random.nextIntBounded(100) // actual computation
      _    <- ZIO.succeed(s"[Task $i read value $res").debugThread
      _   <- lock.releaseRead.commit
    } yield ()


    def write(lock: TReentrantLock) : UIO[Unit] =  for {
      // writer
      _     <- ZIO.sleep(200.millis)
      _     <- ZIO.succeed(s"[writer] is trying to write...").debugThread
      // critical region start
      _    <- lock.acquireWrite.commit
      // critical region start
      _    <- ZIO.succeed(s"[writer] I am able to write").debugThread
      _    <- lock.releaseWrite.commit
      // critical region end
    } yield ()

    for {
      lock  <- TReentrantLock.make.commit
      readersFib    <- ZIO.collectAllDiscard((1 to 10).map(read(_,lock))).fork
      writersFib  <- write(lock).fork
      _   <- readersFib.join
      _   <- writersFib.join
    } yield ()
  }



  override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] = demoReadersAndWriters()


}
