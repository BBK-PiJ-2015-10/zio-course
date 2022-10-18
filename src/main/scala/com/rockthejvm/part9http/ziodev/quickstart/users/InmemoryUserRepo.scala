package com.rockthejvm.part9http.ziodev.quickstart.users

import zio._

import scala.collection.mutable

case class InmemoryUserRepo(map: Ref[mutable.Map[String,User]]) extends UserRepo {

  override def register(user: User): Task[String] = for {
    id <- Random.nextUUID.map(_.toString)
    _  <- map.updateAndGet(_ addOne(id,user))
  } yield id

  override def lookup(id: String): Task[Option[User]] = map.get.map(_.get(id))

  override def users: Task[List[User]] = map.get.map(_.values.toList)

}

object InmemoryUserRepo {

  def layer: ZLayer[Any,Nothing,InmemoryUserRepo] =
    ZLayer.fromZIO(
      Ref.make(mutable.Map.empty[String,User]).map(new InmemoryUserRepo(_))
    )

}
