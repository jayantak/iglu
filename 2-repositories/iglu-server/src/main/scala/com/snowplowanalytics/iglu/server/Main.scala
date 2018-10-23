package com.snowplowanalytics.iglu.server

import cats.effect.IO
import cats.syntax.functor._

import fs2.{Stream, StreamApp}

object Main extends StreamApp[IO] {
  import scala.concurrent.ExecutionContext.Implicits.global

  def stream(args: List[String], requestShutdown: IO[Unit]) = {
    Config.serverCommand.parse(args) match {
      case Right(Config.ServerCommand.Run(config, debug)) =>
        Server.run(config, debug)
      case Right(Config.ServerCommand.Setup(_)) =>
        val exit = IO(println("setup is not supported yet")).as(StreamApp.ExitCode.Error)
        Stream.eval(exit)
      case Left(error) =>
        val exit = IO(println(error)).as(StreamApp.ExitCode.Error)
        Stream.eval(exit)
    }
  }
}
