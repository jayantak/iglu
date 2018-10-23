package com.snowplowanalytics.iglu.server.service

import cats.effect.IO
import cats.implicits._

import io.circe.Json

import org.http4s.circe._
import org.http4s.rho.RhoService
import org.http4s.rho.swagger.syntax.{io => swaggerSyntax}

import com.snowplowanalytics.iglu.server.middleware.PermissionMiddleware
import com.snowplowanalytics.iglu.server.storage.Storage
import com.snowplowanalytics.iglu.server.model.{Codecs, IgluResponse, Permission, Schema, DraftVersion, SchemaDraft}
import com.snowplowanalytics.iglu.server.model.RhoEntities._


class DraftService(db: Storage[IO]) extends RhoService[IO] with Codecs {
  import PermissionMiddleware.Auth.auth
  import SchemaService._
  import swaggerSyntax._

  val version = pathVar[DraftVersion]("version", "Draft version")
  val body = paramD[Boolean]("body", true, "Show full schema, not only Iglu URI")
  val schemaBody = jsonOf[IO, Json]

  "Get a particular draft by its URI" **
    GET / 'vendor / 'name / 'format / version >>> auth() |>> getDraft _

  "Add or update a draft" **
    PUT / 'vendor / 'name / 'format / version >>> auth() ^ schemaBody |>> putDraft _

  "Get list of drafts by vendor and name" **
    GET / 'vendor / 'name >>> auth() |>> getDraftsByName _

  "List all available drafts" **
    GET +? body >>> auth() |>> listDrafts _


  def getDraft(vendor: String, name: String, format: String, version: DraftVersion,
                permission: Permission) = {
    val draftId = SchemaDraft.DraftId(vendor, name, format, version)
    if (permission.canRead(draftId.vendor)) {
      db.getDraft(draftId).flatMap {
        case Some(draft) => Ok(draft)
        case _ => NotFound("bo")
      }
    } else NotFound("foo")
  }

  def getDraftsByName(vendor: String, name: String, permission: Permission) = {
    for {
      schemas <- getSchemas(permission, db.getSchemasByVendorName(vendor, name))
      response <- Ok(schemas)
    } yield response
  }

  def getSchemasByVendor(vendor: String, permission: Permission) =
    for {
      schemas <- getSchemas(permission, db.getSchemasByVendor(vendor, false))
      response <- Ok(schemas)
    } yield response

  def putDraft(vendor: String, name: String, format: String, version: DraftVersion,
               permission: Permission,
               body: Json) = {
    val draftId = SchemaDraft.DraftId(vendor, name, format, version)
    if (permission.canCreate(draftId.vendor))
      db.addDraft(draftId, body) *> Ok("boom")
    else
      Forbidden(IgluResponse.Forbidden: IgluResponse)
  }

  def listDrafts(body: Boolean, permission: Permission) = {
    for {
      schemas <- getSchemas(permission, db.getSchemas)
      response <- if (body)
        Ok(schemas.map(Schema.Representation.apply))
      else
        Ok(schemas.map(_.schemaMap).map(Schema.Representation.apply))
    } yield response
  }
}

