package com.snowplowanalytics.iglu.server
package storage

import java.util.UUID

import io.circe.Json

import cats.effect.Effect
import cats.implicits._

import doobie.Transactor
import doobie.implicits._

import com.snowplowanalytics.iglu.core.SchemaMap
import com.snowplowanalytics.iglu.server.model.{ Permission, Schema, SchemaDraft }
import com.snowplowanalytics.iglu.server.model.SchemaDraft.DraftId

class Postgres[F[_]](xa: Transactor[F]) extends Storage[F] {
  def getSchemaWithMeta(schemaMap: SchemaMap)(implicit F: Effect[F]): F[Option[Schema]] = {
    sql"SELECT schema from schemas where name = ${schemaMap.name}"
      .query[String]
      .option
      .map(_ => none[Schema])
      .transact(xa)
  }

  def getPermission(apikey: UUID)(implicit F: Effect[F]): F[Option[Permission]] = {
    sql"SELECT schema from schemas where name = ${apikey.toString}"
      .query[String]
      .option
      .map(_ => none[Permission])
      .transact(xa)
  }

  def addSchema(schemaMap: SchemaMap, schema: Json)(implicit F: Effect[F]): F[Unit] = {
    sql"SELECT schema from schemas where name = ${schemaMap.toSchemaUri}"
      .query[String]
      .option
      .void
      .transact(xa)
  }

  def getSchemas(implicit F: Effect[F]): F[List[Schema]] = ???

  def getDraft(draftId: DraftId)(implicit F: Effect[F]): F[Option[SchemaDraft]] = ???

  def addDraft(draftId: DraftId, body: Json)(implicit F: Effect[F]): F[Unit] = ???

  def addPermission(uUID: UUID, permission: Permission)(implicit F: Effect[F]): F[Unit] = ???
  def deletePermission(uuid: UUID)(implicit F: Effect[F]): F[Unit] = ???
}

object Postgres {
  def apply[F[_]](xa: Transactor[F]): Postgres[F] = new Postgres(xa)
}
