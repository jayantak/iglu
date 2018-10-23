package com.snowplowanalytics.iglu.server
package model

import cats.effect.Sync
import org.http4s.circe._
import io.circe.Json

import com.snowplowanalytics.iglu.core.SelfDescribingSchema
import com.snowplowanalytics.iglu.core.circe.CirceIgluCodecs._

trait Codecs {

  implicit def representationEntityEncoder[F[_]: Sync] =
    CirceEntityCodec.circeEntityEncoder[F, Schema.Representation]

  implicit def representationListEntityEncoder[F[_]: Sync] =
    CirceEntityCodec.circeEntityEncoder[F, List[Schema.Representation]]

  implicit def stringArrayEntityEncoder[F[_]: Sync] =
    CirceEntityCodec.circeEntityEncoder[F, List[String]]

  implicit def schemaEntityEncoder[F[_]: Sync] =
    CirceEntityCodec.circeEntityEncoder[F, Schema]

  implicit def igluResponseEntityEncoder[F[_]: Sync] =
    CirceEntityCodec.circeEntityEncoder[F, IgluResponse]

  implicit def schemaListEntityEncoder[F[_]: Sync] =
    CirceEntityCodec.circeEntityEncoder[F, List[Schema]]

  implicit def selfDescribingSchemaEntityDecoder[F[_]: Sync] =
    CirceEntityCodec.circeEntityDecoder[F, SelfDescribingSchema[Json]]

  implicit def igluErrorEncoder[F[_]: Sync] =
    CirceEntityCodec.circeEntityEncoder[F, IgluError.SchemaMismatch]

  implicit def draftHttpEncoder[F[_]: Sync] =
    CirceEntityCodec.circeEntityEncoder[F, SchemaDraft]

}

object Codecs extends Codecs
