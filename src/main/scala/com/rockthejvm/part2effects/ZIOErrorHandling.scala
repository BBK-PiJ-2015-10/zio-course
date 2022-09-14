package com.rockthejvm.part2effects

import zio.{Cause, IO, Scope, Task, UIO, URIO, ZIO, ZIOAppArgs, ZIOAppDefault}

import java.io.IOException
import java.net.NoRouteToHostException
import scala.util.{Failure, Success, Try}

object ZIOErrorHandling  extends ZIOAppDefault {

  val aFailedZIO = ZIO.fail("Something went wrong")
  val failedWithThrowable = ZIO.fail(new RuntimeException("culon"))
  val failedWithDescription = failedWithThrowable.mapError(_.getMessage)

  // attempt: run an effect that might throw an exception
  val badZIO = ZIO.succeed {
    println("Trying something")
    val string: String = null
    string.length
  } // This is bad

  val anAttempt: Task[Int] = ZIO.attempt {
    println("Trying something")
    val string: String = null
    string.length
  }

  val catchError: ZIO[Any, Nothing, Any] = anAttempt.catchAll(e => ZIO.succeed("Returning a different value because of $e"))

  val catchSelectiveError: ZIO[Any, Throwable, Any] = anAttempt.catchSome {
    case e: RuntimeException => ZIO.succeed(s"Ignoring runtime exception $e")
    case _ => ZIO.succeed("Ignoring everything else")
  }

  val aBetterAttempt: ZIO[Any, Nothing, Int] = anAttempt.orElse(ZIO.succeed(56))

  val handleBoth: ZIO[Any, Nothing, String] = anAttempt.fold(ex => s"Something bad happened : $ex", value => s"Length of the string was $value")

  val handleBoth2: ZIO[Any, Nothing, String] = anAttempt.foldZIO(
    ex => ZIO.succeed(s"Something bad happened : $ex"),
    value => ZIO.succeed(s"Length of the string was $value")
  )

  /*
  Conversions between Option/Try/Either to ZIO
   */

  val aTryToZio: Task[Int] = ZIO.fromTry(Try(42 / 0)) // can fail with Task

  // either to ZIO
  val anEither: Either[Int, String] = Right("Success!")
  val anEitherToZIO: IO[Int, String] = ZIO.fromEither(anEither)

  // ZIO => ZIO with either as the value channel
  val eitherZIO: URIO[Any, Either[Throwable, Int]] = anAttempt.either
  val anAttempt_v2: ZIO[Any, Throwable, Int] = eitherZIO.absolve

  // option -> ZIO
  val anOption: IO[Option[Nothing], Int] = ZIO.fromOption(Some(42))

  /*
  Exercise : Implement a version fromEither, fromOption, fromEither, esither, absolve
  using fold and foldZIO
   */

  def try2ZIO[A](aTry: Try[A]): Task[A] = {
    aTry match {
      case Failure(exception) => ZIO.fail(exception)
      case Success(value) => ZIO.succeed(value)
    }
  }

  def either2ZIO[E, A](either: Either[E, A]): IO[E, A] = {
    either match {
      case Left(e) => ZIO.fail(e)
      case Right(value) => ZIO.succeed(value)
    }
  }

  def optionToZIO[A](anOption: Option[A]): ZIO[Any, Option[Nothing], A] = {
    anOption match {
      case None => ZIO.fail(None)
      case Some(value) => ZIO.succeed(value)
    }
  }

  def zio2zioEither[R, A, B](zio: ZIO[R, A, B]): ZIO[R, Nothing, Either[A, B]] = {
    zio.fold(
      error => Left(error),
      value => Right(value)
    )
  }

  def absolveZIO[R, A, B](zio: ZIO[R, Nothing, Either[A, B]]): ZIO[R, A, B] = {
    zio.flatMap {
      case Left(e) => ZIO.fail(e)
      case Right(value) => ZIO.succeed(value)
    }
  }
  /*
    Errors = failures present in the zio type signature("checked" exceptions)
    Defects = failures that are unrecoverable, unforseen, and NOT present in the ZIO type signatures

    ZIO[R,E,A] can finish with Exit[E,A[
       - Success containing A
       - Cause[E]
         - Fail[E] containing the error
         - Die(t: Throwable) which was unforseen
   */

  def divisionByZero: UIO[Int] = ZIO.succeed(1 / 0)

  val failedInt: ZIO[Int, String, Int] = ZIO.fail("I failed")

  val fuckerIt: IO[RuntimeException, Nothing] = ZIO.fail(new RuntimeException("fucker"))

  val failureCauseExposed: ZIO[Int, Cause[String], Int] = failedInt.sandbox

  val failureCauseHidden: ZIO[Int, String, Int] = failureCauseExposed.unsandbox

  // fold with cause
  val foldedWithCause: URIO[Int, String] = failedInt
    .foldCause(
      cause => s"this failed with ${cause.defects}",
      value => s"woof $value")

  val foldWithCause_v1: ZIO[Int, Exception, String] = failedInt.foldCauseZIO(
    cause => ZIO.fail(new Exception(s"this failed with ${cause.defects}")),
    value => ZIO.succeed(s"this succeeded with $value")
  )

  def callHttpEndpoint(url: String): ZIO[Any, IOException, String] =
    ZIO.fail(new IOException("no internet, dummy"))

  def callHttpEndpointWideError(url: String): ZIO[Any, Exception, String] =
    ZIO.fail(new IOException("no internet, dummy"))

  def callHttpEndpoint_v2(url: String): ZIO[Any, IOException, String] =
    callHttpEndpointWideError(url).refineOrDie[IOException] {
      case e: IOException => e
      case _: NoRouteToHostException => new IOException(s"No route to host to $url")
    }

  val endpointCallWithDefects: ZIO[Any, Nothing, String] =
    callHttpEndpoint("rockthejvm.com").orDie // all errors are now defects

  // reverse turn defects into the error channel

  val endpointCallWithError: ZIO[Any, RuntimeException, String] = endpointCallWithDefects.unrefine {
    case e => new RuntimeException(e.getMessage)
  }

  /*
  Combined effects with different errors
   */

  case class IndexError(message: String)

  case class DBError(message: String)

  val callApi: ZIO[Any, IndexError, String] = ZIO.succeed("page: <html></html")
  val queryDb: ZIO[Any, DBError, Int] = ZIO.succeed(1)

  val combined = for {
    page <- callApi
    rows <- queryDb
  } yield (page, rows)

  /**
   * Solutions:
   *  - design error model
   *  - use Scala 3 union types
   *  - .mapError to some error type
   *
   */

  // 1 - make this effect fail with TYPED error
  val aBadFailure: ZIO[Any, Nothing, Int] = ZIO.succeed[Int](throw new RuntimeException("this is bad!"))
  //    val aBadFailure: ZIO[Any, Exception, Int] = ZIO.succeed[Int](throw new RuntimeException("this is bad!")).unrefine{
  //      dog => new Exception(dog)
  //    }

  val aBetterFailure: ZIO[Any, Cause[Nothing], Int] = aBadFailure.sandbox
  val aBetterFailure2: ZIO[Any, Throwable, Int] = aBadFailure.unrefine {
    case e => e
  }


  // 2 - Narrowing the exception error
  def IOException[R, A](myZio: ZIO[R, Throwable, A]): ZIO[R, IOException, A] =
    myZio.foldCauseZIO(
      bad => ZIO.fail(new IOException(bad.defects.head)),
      good => ZIO.succeed(good)
    )

  def IOException2[R, A](myZio: ZIO[R, Throwable, A]): ZIO[R, IOException, A] =
    myZio.refineOrDie{
      case io: Exception => new IOException(io)
    }


  //left on 22:43
//  // 3 - Either and exposed
  def left[R,E,A,B](zio: ZIO[R,E,Either[A,B]]): ZIO[R,Either[E,A],B] = {
    zio.foldZIO(
      e => ZIO.fail(Left(e)),
      either => either match {
        case Left(e) => ZIO.fail(Right(e))
        case Right(value) => ZIO.succeed(value)
      }
    )
  }

  // 4
  val database = Map(
    "daniel" -> 43,
     "alice" -> 789
  )

  case class QueryError(reason: String)
  case class UserProfile(name: String, phone: Int)

  def lookUpProfile(userId: String): ZIO[Any,QueryError,Option[UserProfile]] =
    if (userId != userId.toLowerCase()){
      ZIO.fail(QueryError("user ID format is invalid"))
    } else {
      ZIO.succeed(database.get(userId).map(phone => UserProfile(userId,phone)))
    }

  // surface all the failed cases of this API

  def betterLookUpProfile(userId: String) : ZIO[Any,Option[QueryError],UserProfile] =
    lookUpProfile(userId).foldZIO(
      error => ZIO.fail(Some(error)),
      result => result match {
        case None => ZIO.fail(None)
        case Some(value) => ZIO.succeed(value)
      }
    )


  def betterLookUpProfile_v2(userId: String) : ZIO[Any,Option[QueryError],UserProfile] =
    lookUpProfile(userId).some













  //  def ZIO2Either[E,A](zio: IO[E,A]): Either[E,A] = {
//    zio.fold(
//      e => Left(e),
//      other => Right(other)
//    )
//  }





  override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] = ???


}
