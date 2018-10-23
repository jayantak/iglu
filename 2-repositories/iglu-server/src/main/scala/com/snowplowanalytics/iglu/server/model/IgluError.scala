package com.snowplowanalytics.iglu.server.model

import cats.Monad
import com.snowplowanalytics.iglu.core.SchemaMap
import io.circe.{ Encoder, Json }
import org.http4s.{DecodeFailure, HttpVersion, Response, Status}

object IgluError {
  /** Mismatch between SchemaMap in URI/path and content */
  case class SchemaMismatch(path: SchemaMap, content: SchemaMap) extends DecodeFailure {
    def cause: Option[Throwable] = None

    def toHttpResponse[F[_]](httpVersion: HttpVersion)(implicit F: Monad[F]): F[Response[F]] =
      F.pure(Response(Status.BadRequest))

    def message: String = "Schemas mismatched"
  }

  implicit val mimstchEncoder = Encoder.instance[SchemaMismatch] { _ => Json.Null }

  case class DecodeError(message: String)
}
