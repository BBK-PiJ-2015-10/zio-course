package com.rockthejvm.part9http.ziodev.quickstart.users

import io.getquill.context.ZioJdbc.DataSourceLayer
import io.getquill.{Escape,H2ZioJdbcContext}
import io.getquill.jdbczio.Quill
import io.getquill._
import zio._

import java.util.UUID
import javax.sql.DataSource

case class UserTable(uuid: UUID,name: String,age: Int)

case class PersistentUserRepo(dataSource: DataSource) extends UserRepo {

  val ctx = new H2ZioJdbcContext(Escape)

  import ctx._

  override def register(user: User): Task[String] = {
    for {
      id <- Random.nextUUID
      _ <- ctx.run {
        quote {
          query[UserTable].insertValue {
            lift(UserTable(id,user.name,user.age))
          }
        }
      }
    } yield id.toString
  }.provide(ZLayer.succeed(dataSource))

  override def lookup(id: String): Task[Option[User]] = {
       ctx.run {
        quote {
          query[UserTable]
            .filter(t => t.uuid == lift(UUID.fromString(id)))
            .map(u => User(u.name,u.age))
        }
      }
  }.provide(ZLayer.succeed(dataSource)).map(_.headOption)

  override def users: Task[List[User]] = {
   ctx.run {
     quote {
       query[UserTable]
         .map(u => User(u.name,u.age))
     }
   }
  }.provide(ZLayer.succeed(dataSource))


}

object PersistentUserRepo {
  def layer: ZLayer[Any,Throwable,PersistentUserRepo] =
    Quill.DataSource.fromPrefix("UserApp") >>>
      ZLayer.fromFunction(PersistentUserRepo(_))
}
