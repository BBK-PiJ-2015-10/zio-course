package com.rockthejvm.part3concurrency

import com.rockthejvm.utilsScala2.DebugWrapper
import zio._

import java.util.concurrent.{ExecutorService, Executors}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

object AsynchronousEffects extends ZIOAppDefault {

  // CALLBACK - based
  object LoginService {
    case class AuthError(message: String)
    case class UserProfile(email: String, name: String)

    // thread pool
    val executor = Executors.newFixedThreadPool(8)

    // "database"
    val passwd = Map(
      "da@rockthejvm.com" -> "RockTheJVM1!"
    )

    // the profile data
    val database = Map(
      "da@rockthejvm.com"-> "Da"
    )

    def login(email: String, password: String)(onSuccess: UserProfile => Unit,onFailure: AuthError => Unit) =
      executor.execute { () =>
        println(s"[${Thread.currentThread().getName}] Attempting login for $email")
        passwd.get(email) match {
          case Some(`password`) =>
            onSuccess(UserProfile(email,database(email)))
          case Some(f) =>
            onFailure(AuthError(s"Incorrect password $f"))
          case None =>
            onFailure(AuthError(s"User with email $email does not exist"))
        }
      }

  }

  def loginAsZIO(id: String, pw: String): ZIO[Any,LoginService.AuthError,LoginService.UserProfile] =
    ZIO.async[Any,LoginService.AuthError,LoginService.UserProfile] { cb => // callback object created by ZIO
      LoginService.login(id,pw)(
        profile => cb(ZIO.succeed(profile)),
        error   => cb(ZIO.fail(error))
      )
    }

  val loginProgram = for {
    email <- Console.readLine("Email: ")
    pass  <- Console.readLine("Password: ")
    profile <- loginAsZIO(email,pass).debugThread
    _       <- Console.printLine(s"Welcome to Rock the JVM, ${profile.name}")
  } yield ()

  /**
   * Exercise
   *
   */
  // 1 - surface a computation running on some (external) thread to zio
    val demoExternal2ZIO = {
        val executor = Executors.newFixedThreadPool(8)
        val zio = external2ZIO({ () =>
          println(s"[${Thread.currentThread().getName}] computing the meaning of life on some thread")
          Thread.sleep(1000)
          42
        })(executor)
        zio.debugThread.unit
      }

   def external2ZIO[A](computation: () => A)(executor: ExecutorService) : Task[A] =
     ZIO.async[Any,Throwable,A] { cb =>
       executor.execute{ () =>
         try {
           val result = computation()
           cb(ZIO.succeed(result))
         } catch {
           case e: Throwable => cb(ZIO.fail(e))
         }
       }
     }

  //def future2ZIO[A](future: => Future[A])(implicit ec: ExecutionContext): Task[A] =
  def future2ZIO[A](future: => Future[A])(implicit ec: ExecutionContext): Task[A] =
    ZIO.async[Any,Throwable,A] { cb =>
      future.onComplete{
        case Success(value) => cb(ZIO.succeed(value))
        case Failure(exception) => cb(ZIO.fail(exception))
      }
    }

  val demoFuture2ZIO = {
    val executor = Executors.newFixedThreadPool(8)
    implicit val ec = ExecutionContext.fromExecutorService(executor)
    val mol: Task[Int] = future2ZIO(Future {
      println(s"[${Thread.currentThread().getName}] computing the meaning of life on some thread")
      Thread.sleep(1000)
      42
    })
    mol.debugThread.unit
  }

  // 3 - never ending zio
  def neverEndingZIO[A]: UIO[A] = ZIO.async[Any,Nothing,A]{ cb =>
    println("woof woof")
  }

  val never = ZIO.never

  override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] =
    ZIO.succeed("Computing...").debugThread *> neverEndingZIO[Int] *> ZIO.succeed("Completed.").debugThread

}
