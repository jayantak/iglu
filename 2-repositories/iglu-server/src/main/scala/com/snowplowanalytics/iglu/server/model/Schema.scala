package com.snowplowanalytics.iglu.server.model

import java.time.Instant

import com.snowplowanalytics.iglu.core.{SchemaKey, SchemaMap}
import com.snowplowanalytics.iglu.core.circe.CirceIgluCodecs.encodeSchemaMap

import io.circe.{ Json, Encoder }
import io.circe.syntax._
import io.circe.java8.time._
import io.circe.generic.semiauto._

import Schema.Metadata

case class Schema(schemaMap: SchemaMap, metadata: Metadata, body: Json)

object Schema {

  case class Metadata(createdAt: Instant, updatedAt: Instant, isPublic: Boolean)

  object Metadata {
    implicit val encoder: Encoder[Metadata] =
      deriveEncoder[Metadata]
  }

  sealed trait Representation
  object Representation {
    case class Full(schema: Schema) extends Representation
    case class Uri(schemaKey: SchemaKey) extends Representation

    def apply(schema: Schema): Representation = Full(schema)
    def apply(uri: SchemaMap): Representation = Uri(uri.toSchemaKey)
  }

  implicit val schemaEncoder: Encoder[Schema] =
    Encoder.instance { schema =>
      Json.obj("self" -> schema
        .schemaMap
        .asJson(encodeSchemaMap)
        .deepMerge(schema.metadata.asJson(Metadata.encoder))
      ).deepMerge(schema.body)
    }

  implicit val representationEncoder: Encoder[Representation] =
    Encoder.instance {
      case Representation.Full(s) => schemaEncoder.apply(s)
      case Representation.Uri(u) => Encoder[String].apply(u.toSchemaUri)
    }
}
