package com.snowplowanalytics.iglu.server

import cats.effect.IO
import fs2.{Stream, StreamApp}

import org.http4s.server.blaze.BlazeBuilder
import org.http4s.rho.bits.PathAST.{PathMatch, TypedPath}
import org.http4s.rho.swagger.syntax.{io => ioSwagger}
import org.http4s.rho.swagger.models.{ ApiKeyAuthDefinition, In }

import com.snowplowanalytics.iglu.server.storage.Storage
import com.snowplowanalytics.iglu.server.service._

import scala.concurrent.ExecutionContext

object Server {

  val swagger = ioSwagger.createRhoMiddleware(
    apiPath = TypedPath(PathMatch("swagger.json")),
    securityDefinitions = Map("apikey" -> ApiKeyAuthDefinition("apikey", In.HEADER))
  )

  val schemasSwagger = ioSwagger.createRhoMiddleware(
    apiPath = TypedPath(PathMatch("swagger.json")),
    basePath = Some("/api/schemas"),
    securityDefinitions = Map("Iglu API key" -> ApiKeyAuthDefinition("apikey", In.HEADER))
  )


  def attachDebug(builder: BlazeBuilder[IO], storage: Storage[IO]) =
    builder.mountService(DebugService.asService(storage, swagger), "/api/debug")

  def run(config: Config, debug: Boolean)(implicit ec: ExecutionContext): Stream[IO, StreamApp.ExitCode] =
    for {
      storage <- Stream.eval(Storage.initialize[IO](config.database))
      baseBuilder = BlazeBuilder[IO]
        .bindHttp(config.http.port, config.http.interface)
        .mountService(SchemaService.asService(storage, schemasSwagger), "/api/schemas")
        .mountService(AuthService.asService(storage, swagger), "/api/auth")
        .mountService(ValidationService.asService(swagger), "/api/validation")
        .mountService(StaticContentService.routes)
      builder = if (debug) attachDebug(baseBuilder, storage) else baseBuilder
      stream <- builder.serve
    } yield stream
}
