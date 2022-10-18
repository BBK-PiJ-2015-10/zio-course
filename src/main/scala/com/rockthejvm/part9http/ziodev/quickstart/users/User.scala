package com.rockthejvm.part9http.ziodev.quickstart.users

import zio.json._

case class User(name: String,age: Int)

object User {

   implicit val toJsonEncoderUser =  DeriveJsonEncoder.gen[User]

   implicit val fromJsonDecoderUser = DeriveJsonDecoder.gen[User]

}
