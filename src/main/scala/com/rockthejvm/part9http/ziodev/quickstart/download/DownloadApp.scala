package com.rockthejvm.part9http.ziodev.quickstart.download

import zhttp.http._
import zio._
import zio.stream.ZStream

object DownloadApp {

  def apply(): Http[Any,Throwable,Request,Response] =
    Http.collectHttp[Request] {

      // GET /download
      case Method.GET -> !! / "download" =>
        val fileName = "file.txt"
        Http.fromStream(ZStream.fromResource(fileName))
          .setHeaders(
            Headers(
              ("Content-Type","application/octet-stream"),
              ("Content-Disposition",s"attachment; filename=${fileName}"),
            ))

      case Method.GET -> !! / "download" / "stream" =>
        val fileName = "bigfile.txt"
        Http.fromStream(ZStream.fromResource(fileName).schedule(Schedule.spaced(50.millis))
        ).setHeaders(
          Headers(
            ("Content-Type","application/octet-stream"),
            ("Content-Disposition",s"attachment: filename=$fileName")
          )
        )

    }

}
