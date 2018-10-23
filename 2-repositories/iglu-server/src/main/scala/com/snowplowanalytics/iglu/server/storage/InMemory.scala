package com.snowplowanalytics.iglu.server.storage

import java.util.UUID

import cats.implicits._
import cats.effect.Effect

import fs2.async.Ref

import io.circe.Json

import com.snowplowanalytics.iglu.core.SchemaMap

import com.snowplowanalytics.iglu.server.model.{ Permission, Schema, SchemaDraft }
import com.snowplowanalytics.iglu.server.model.SchemaDraft.DraftId

/** Ephemeral storage that will be lost after server shut down */
case class InMemory[F[_]](ref: Ref[F, InMemory.State]) extends Storage[F] {
  def getSchemaWithMeta(schemaMap: SchemaMap)(implicit F: Effect[F]): F[Option[Schema]] =
    for { db <- ref.get } yield db.schemas.get(schemaMap)

  def getPermission(apiKey: UUID)(implicit F: Effect[F]): F[Option[Permission]] =
    for { db <- ref.get } yield db.permission.get(apiKey)

  def addSchema(schemaMap: SchemaMap, body: Json)(implicit F: Effect[F]): F[Unit] =
    for {
      db <- ref.get
      addedAt <- getTime
      meta = Schema.Metadata(addedAt, addedAt, true)
      schema = Schema(schemaMap, meta, body)
      _ <- ref.modify(_.copy(schemas = db.schemas.updated(schemaMap, schema)))
    } yield ()

  def getSchemas(implicit F: Effect[F]): F[List[Schema]] =
    ref.get.map(_.schemas.values.toList)

  def getDraft(draftId: DraftId)(implicit F: Effect[F]): F[Option[SchemaDraft]] =
    F.suspend { println(draftId); ??? }

  def addDraft(draftId: DraftId, body: Json)(implicit F: Effect[F]): F[Unit] =
    F.suspend { println(draftId); println(body); ??? }

  def addPermission(apikey: UUID, permission: Permission)(implicit F: Effect[F]): F[Unit] =
    for {
      db <- ref.get
      _ <- ref.modify(_.copy(permission = db.permission.updated(apikey, permission)))
    } yield ()

  def deletePermission(apikey: UUID)(implicit F: Effect[F]): F[Unit] =
    for {
      db <- ref.get
      _ <- ref.modify(_.copy(permission = db.permission - apikey))
    } yield ()
}

object InMemory {
  case class State(schemas: Map[SchemaMap, Schema],
                   permission: Map[UUID, Permission])

  object State {
    val empty: State = State(Map.empty, Map.empty)

    /** Dev state */
    val withMasterKey: State =
      State(
        Map.empty[SchemaMap, Schema],
        Map(UUID.fromString("48b267d7-cd2b-4f22-bae4-0f002008b5ad") -> Permission.master)
      )
  }

  def get[F[_]: Effect](fixture: State): F[Storage[F]] =
    for { db <- Ref[F, State](fixture) } yield InMemory[F](db)

  def empty[F[_]: Effect]: F[Storage[F]] =
    for { db <- Ref[F, State](State.withMasterKey) } yield InMemory[F](db)

  /** Equal to `get`, but doesn't lose the precise return type */
  def getInMemory[F[_]: Effect](fixture: State): F[InMemory[F]] =
    for { db <- Ref[F, State](fixture) } yield InMemory[F](db)
}
