package com.snowplowanalytics.iglu.server.service

import cats.effect.IO

import io.circe.Json

import org.http4s.HttpService
import org.http4s.circe._
import org.http4s.rho.{RhoMiddleware, RhoService}
import org.http4s.rho.swagger.syntax.{io => swaggerSyntax}

class ValidationService extends RhoService[IO] {
  import swaggerSyntax._

  "This route allows you to validate schemas" **
    POST / "validate" / "jsonschema" ^ jsonDecoder[IO] |>> {schema: Json => validateSchema(schema) }


  def validateSchema(schema: Json) = {
    if (true) Ok(schema) else NotFound(Json.fromString("foo"))
  }
}

object ValidationService {
  def asService(rho: RhoMiddleware[IO]): HttpService[IO] =
    (new ValidationService).toService(rho)
}
