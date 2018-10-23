package com.snowplowanalytics.iglu.server
package service

import cats.FlatMap
import cats.effect.IO
import cats.implicits._

import io.circe.Json

import org.http4s.HttpService
import org.http4s.circe._
import org.http4s.rho.{RhoMiddleware, RhoService}
import org.http4s.rho.swagger.syntax.{io => swaggerSyntax}

import com.snowplowanalytics.iglu.core.{SchemaMap, SchemaVer, SelfDescribingSchema}
import com.snowplowanalytics.iglu.core.circe.CirceIgluCodecs._

import com.snowplowanalytics.iglu.server.middleware.PermissionMiddleware
import com.snowplowanalytics.iglu.server.storage.Storage
import com.snowplowanalytics.iglu.server.model.{Codecs, IgluError, IgluResponse, Permission, Schema}
import com.snowplowanalytics.iglu.server.model.RhoEntities._

class SchemaService(db: Storage[IO]) extends RhoService[IO] with Codecs {
  import swaggerSyntax._
  import SchemaService._
  import PermissionMiddleware.Auth.auth

  val version = pathVar[SchemaVer.Full]("version", "SchemaVer")
  val body = paramD[Boolean]("body", true, "Show full schema, not only Iglu URI")
  val schemaBody = jsonOf[IO, SelfDescribingSchema[Json]]

  "Get a particular schema by its Iglu URI" **
    GET / 'vendor / 'name / 'format / version >>> auth() |>> getSchema _

  "Add a schema (self-describing or not) to its Iglu URI" **
    PUT / 'vendor / 'name / 'format / version >>> auth() ^ schemaBody |>> putSchema _

  "Get list of schemas by vendor name" **
    GET / 'vendor / 'name >>> auth() |>> getSchemasByName _

  "Get all schemas for vendor" **
    GET / 'vendor >>> auth() |>> getSchemasByVendor _

  "Publish new self-describing schema" **
    POST >>> auth() ^ schemaBody |>> publishSchema _

  "List all available schemas" **
    GET +? body >>> auth() |>> listSchemas _


  def getSchema(vendor: String, name: String, format: String, version: SchemaVer.Full,
                permission: Permission) = {
    db.getSchemaWithMeta(SchemaMap(vendor, name, format, version)).flatMap {
      case Some(schema) if schema.metadata.isPublic => Ok(schema.body)
      case Some(schema) if permission.canRead(schema.schemaMap.vendor) => Ok(schema.body)
      case _ => NotFound("bo")
    }
  }

  def getSchemasByName(vendor: String, name: String, permission: Permission) = {
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

  def publishSchema(permission: Permission, schema: SelfDescribingSchema[Json]) =
    if (permission.canCreate(schema.self.vendor)) db.addSchema(schema.self, schema.schema) *> Ok("boom!")
    else Forbidden(IgluResponse.Forbidden: IgluResponse)

  def putSchema(vendor: String, name: String, format: String, version: SchemaVer.Full,
                permission: Permission,
                schema: SelfDescribingSchema[Json]) = {
    val schemaMapUri = SchemaMap(vendor, name, format, version)
    if (schemaMapUri == schema.self) {
      val authorized = permission.canCreate(schemaMapUri.vendor)
      if (authorized)
        db.addSchema(schema.self, schema.schema) *>
          Ok(IgluResponse.SchemaUploaded("updated", Some(schemaMapUri.toSchemaUri)): IgluResponse)
      else
        Forbidden(IgluResponse.Forbidden: IgluResponse)
    } else BadRequest(IgluError.SchemaMismatch(schemaMapUri, schema.self))
  }

  def listSchemas(body: Boolean, permission: Permission) = {
    for {
      schemas <- getSchemas(permission, db.getSchemas)
      response <- if (body)
        Ok(schemas.map(Schema.Representation.apply))
      else
        Ok(schemas.map(_.schemaMap).map(Schema.Representation.apply))
    } yield response
  }
}

object SchemaService {
  def asService(db: Storage[IO], rhoMiddleware: RhoMiddleware[IO]): HttpService[IO] = {
    val service = new SchemaService(db).toService(rhoMiddleware)
    PermissionMiddleware.wrapService(db, service)
  }

  /** Extract schemas from database, available for particular permission */
  def getSchemas[F[_]: FlatMap](permission: Permission, query: F[List[Schema]]): F[List[Schema]] =
    for {
      schemas <- query
      filtered = schemas.filter {
        case Schema(key, meta, _) => permission.canRead(key.vendor) || meta.isPublic
      }
    } yield filtered
}