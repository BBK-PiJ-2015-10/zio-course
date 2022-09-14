package com.rockthejvm.part2effects

import zio._
import zio.ZIOAppDefault

import java.util.concurrent.TimeUnit

object ZioDependencies extends ZIOAppDefault{

  // app to subscribe users to newsletter

  case class User(name: String, email: String)

  class UserSubscription(emailService: EmailService, userDatabase: UserDatabase) {
    def subscribeUser(user: User) : Task[Unit] = for {
      _ <- emailService.email(user)
      _ <- userDatabase.insert(user)
    } yield ()
  }

  object UserSubscription {

    def apply(emailService: EmailService, userDatabase: UserDatabase) = new UserSubscription(emailService,userDatabase)

    val live: ZLayer[EmailService with UserDatabase,Nothing,UserSubscription] = {
      ZLayer.fromFunction(apply(_,_))
    }

  }

  class EmailService {

    def email(user: User): Task[Unit] = ZIO.succeed(println(s"You have just been subscribed, ${user.name}"))



  }

  object EmailService {

    def apply() = new EmailService()

    val live: ZLayer[Any,Nothing,EmailService] = ZLayer.succeed(apply())

  }

  class UserDatabase(connectionPool: ConnectionPool) {
    def insert(user: User) : Task[Unit] = {
      for {
        conn <- connectionPool.get
        -    <- conn.runQuery(s"insert into subscribers(name,email) values (${user.name},${user.email})")
      } yield ()
    }
  }

  object UserDatabase {
    def apply(connectionPool: ConnectionPool) = new UserDatabase(connectionPool )

    val live: ZLayer[ConnectionPool,Nothing,UserDatabase] = ZLayer.fromFunction(apply(_))
  }

  class ConnectionPool(nConnections: Int) {
    def get: Task[Connection] = ZIO.succeed(println("Acquired connection")) *> ZIO.succeed(Connection())
  }

  object ConnectionPool {
    def apply(nConnections: Int) = new ConnectionPool(nConnections)

    def live(nConnections: Int): ZLayer[Any,Nothing,ConnectionPool] = ZLayer.succeed(apply(nConnections))

  }

  case class Connection(){
    def runQuery(query: String): Task[Unit] = ZIO.succeed(println(s"Executing query: $query"))
  }

  object Connection {
    def apply() = new Connection()

    val live: ZLayer[Any,Nothing,Connection] = ZLayer.succeed(apply())
  }

  // dependency injections
  val subscriptionService = ZIO.succeed(
    new UserSubscription(
      new EmailService(),
      new UserDatabase(
        new ConnectionPool(10)
      )
    )
  )

  /**
   *
   * - massive
   * - DI can be 100x more
   * - not having all deps in the same place
   * - passing dependencies multiple times
   * -
   */
  val subscriptionService2 = ZIO.succeed(
    UserSubscription(
      EmailService(),
      UserDatabase(
        ConnectionPool(10)
      )
    )
  )

  def subscribe(user: User) : Task[Unit] = for {
    sub <- subscriptionService2 // service is instantiated at the point of call
    _   <- sub.subscribeUser(user)
  } yield ()

  //risk leaking resources if you subscribe multimple users in the same program

  val program = for {
    _  <- subscribe(User("ale","yasserpo@hotmail.com"))
    <  <- subscribe(User("Hell","other@gmail.com"))
  } yield ()

  def subscribe_v2(user: User): ZIO[UserSubscription, Throwable, Unit] = for {
    sub <- ZIO.service[UserSubscription]
    _   <- sub.subscribeUser(user)
  } yield ()

  val program_v2 = for {
    _  <- subscribe_v2(User("ale","yasserpo@hotmail.com"))
    <  <- subscribe_v2(User("Hell","other@gmail.com"))
  } yield ()

  /**
   * ZLayer
   *
   */

  val connectionPoolLayer: ZLayer[Any,Nothing,ConnectionPool] = ZLayer.succeed(ConnectionPool.apply(10))

  val databaseLayer: ZLayer[ConnectionPool, Nothing, UserDatabase] = ZLayer.fromFunction(UserDatabase(_))

  val emailServiceLayer: ZLayer[Any,Nothing,EmailService] = ZLayer.succeed(EmailService.apply())

  val userSubscriptionServiceLayer: ZLayer[EmailService with UserDatabase, Nothing, UserSubscription] = ZLayer.fromFunction(UserSubscription(_,_))

  // composing layers
  // vertical composition >>> operator to use
  val databaseLayerFull: ZLayer[Any,Nothing,UserDatabase] = connectionPoolLayer >>> databaseLayer

  //horizontal composition
  // combine the dependency of both layers
  // combine the error channel, lowest commont ancestor
  val subscriptionRequirementsLayer = databaseLayerFull ++ emailServiceLayer

  val userSubscriptionLayer = subscriptionRequirementsLayer >>> userSubscriptionServiceLayer

  val userSubscriptionLayer_v2 : ZLayer[Any,Nothing,UserSubscription] = ZLayer.make[UserSubscription](
    UserSubscription.live,
    EmailService.live,
    UserDatabase.live,
    ConnectionPool.live(10)
  )
 // magic

  val runnableProgram = program_v2.provide(userSubscriptionLayer)

  val runnableProgram_v2 = program_v2.provide(
    UserSubscription.live,
    EmailService.live,
    // if you want to skip memoization
    UserDatabase.live.fresh,
    ConnectionPool.live(10),
    ZLayer.Debug.mermaid,
    //ZLayer.Debug.tree
  )

  // passthrough
  val dbWithPoolLayer : ZLayer[ConnectionPool,Nothing,ConnectionPool with UserDatabase]  = UserDatabase.live.passthrough
  // service = take a dep and expose it as a value to further layers
  val dbService = ZLayer.service[UserDatabase]
  // launch
  val subscriptionLaunch: ZIO[EmailService with UserDatabase,Nothing,Nothing] = UserSubscription.live.launch
  // memoization

  val getTime = Clock.currentTime(TimeUnit.SECONDS)
  val randomValue = Random.nextInt
  val systeVariable = System.env("dogother")
  val printlnEffect = Console.printLine("This is a print")




  override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] =

    runnableProgram_v2

    //program_v2.provide(userSubscriptionLayer)
//    program_v2.provide(
//      ZLayer.succeed(
//        UserSubscription(
//          EmailService(),
//          UserDatabase(
//            ConnectionPool(10)
//          )
//        )
//      )
//    )
  //subscribe(User("Alex",("yasserpo@hotmail.com")))

}
