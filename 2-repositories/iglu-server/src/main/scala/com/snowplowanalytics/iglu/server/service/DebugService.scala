package com.snowplowanalytics.iglu.server.service

import cats.effect.IO

import org.http4s.rho.{ RhoService, RhoMiddleware }
import org.http4s.rho.swagger.syntax.{io => swaggerSyntax}


import com.snowplowanalytics.iglu.server.storage.{ Storage, InMemory }

/** Service showing whole in-memory state. Use for development only */
class DebugService(db: Storage[IO]) extends RhoService[IO] {
  import swaggerSyntax._

  "Show internal state" **
    GET |>> {
    db match {
      case InMemory(ref) =>
        for {
          db <- ref.get
          response <- Ok(db.toString)
        } yield response
      case other => NotImplemented(s"Cannot show $other")
    }
  }
}

object DebugService {
  def asService(db: Storage[IO], rhoMiddleware: RhoMiddleware[IO]) =
    new DebugService(db).toService(rhoMiddleware)
}
