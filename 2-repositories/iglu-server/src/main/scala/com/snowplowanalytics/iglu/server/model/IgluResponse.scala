package com.snowplowanalytics.iglu.server.model

import java.util.UUID

import io.circe.{Encoder, Json}

import com.snowplowanalytics.iglu.core.SchemaKey

trait IgluResponse extends Product with Serializable

object IgluResponse {

  case object SchemaNotFound extends IgluResponse
  case object Forbidden extends IgluResponse
  case class ApiKeys(read: UUID, write: UUID) extends IgluResponse
  case class SchemaMismatch(uriSchemaKey: SchemaKey, payloadSchemaKey: SchemaKey) extends IgluResponse
  case object InvalidSchema extends IgluResponse
  case class SchemaUploaded(message: String, location: Option[String]) extends IgluResponse

  implicit val responsesEncoder: Encoder[IgluResponse] =
    Encoder.instance {
      case SchemaNotFound =>
        Json.fromFields(List("message" -> Json.fromString("Schema not found")))
      case Forbidden =>
        Json.fromFields(List("message" -> Json.fromString("Not authorized")))
      case ApiKeys(read, write) =>
        Json.fromFields(List(
          "read" -> Json.fromString(read.toString),
          "write" -> Json.fromString(write.toString)
        ))
      case SchemaMismatch(uri, payload) =>
        Json.fromFields(List(
          "uriSchemaKey" -> Json.fromString(uri.toSchemaUri),
          "payloadSchemaKey" -> Json.fromString(payload.toSchemaUri),
          "message" -> Json.fromString("Schema metadata mismatches in payload and URI")
        ))
      case SchemaUploaded(message, location) =>
        Json.fromFields(List(
          "message" -> Json.fromString(message),
          "location" -> location.map(Json.fromString).getOrElse(Json.Null)
        ))
      case InvalidSchema =>
        Json.fromFields(List("message" -> Json.fromString("Cannot decode JSON Schema")))
    }
}
