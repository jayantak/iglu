package com.snowplowanalytics.iglu.server.model

import io.circe.{ Json, Encoder }
import io.circe.generic.semiauto.deriveEncoder
import io.circe.refined._

import com.snowplowanalytics.iglu.server.model.SchemaDraft.DraftId

import Schema.Metadata

case class SchemaDraft(schemaMap: DraftId, metadata: Metadata, body: Json)

object SchemaDraft {
  case class DraftId(vendor: String, name: String, format: String, version: DraftVersion)

  implicit def draftIdEncoder: Encoder[DraftId] =
    deriveEncoder[DraftId]

  implicit def draftEncoder: Encoder[SchemaDraft] =
    deriveEncoder[SchemaDraft]

}
