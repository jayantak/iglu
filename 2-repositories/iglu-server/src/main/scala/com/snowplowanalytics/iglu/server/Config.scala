package com.snowplowanalytics.iglu.server

import cats.implicits._

import com.monovore.decline._


case class Config(database: Config.StorageConfig, http: Config.Http)

object Config {

  sealed trait StorageConfig
  object StorageConfig {
    case object DummyConfig extends StorageConfig
    case class PostgresConfig(host: String, port: Int, name: String, username: String, password: String) extends StorageConfig
  }

  case class Http(interface: String, baseUrl: String, port: Int)

  val empty = Config(StorageConfig.DummyConfig, Http("0.0.0.0", "", 8088))

  sealed trait ServerCommand
  object ServerCommand {
    case class Run(config: Config, debug: Boolean) extends ServerCommand
    case class Setup(config: Config) extends ServerCommand
  }

  val configOpt = Opts.option[String]("config", "Server configuration JSON").map { _ =>
    empty
  }
  val debugFlag = Opts.flag("debug", "Enable extensive logging and debug endpoint").orFalse

  val runCommandOpt = (configOpt, debugFlag).mapN(ServerCommand.Run.apply)

  val runCommand: Opts[ServerCommand] =
    Opts.subcommand("run", "Run Iglu Server")(runCommandOpt)
  val setupCommand: Opts[ServerCommand] =
    Opts.subcommand("setup", "Setup Iglu Server")(configOpt).map(ServerCommand.Setup.apply)

  val serverCommand = Command[ServerCommand]("iglu-server", "0.1.0")(runCommand.orElse(setupCommand))
}
