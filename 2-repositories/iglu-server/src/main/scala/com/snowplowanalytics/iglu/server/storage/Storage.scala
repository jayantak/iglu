package com.snowplowanalytics.iglu.server
package storage

import java.util.UUID
import java.time.Instant

import cats.effect.{ Effect, Sync }
import cats.implicits._

import io.circe.Json

import doobie.hikari._

import com.snowplowanalytics.iglu.core.SchemaMap
import com.snowplowanalytics.iglu.server.Config.StorageConfig
import com.snowplowanalytics.iglu.server.model.{ Permission, Schema, SchemaDraft  }

import com.snowplowanalytics.iglu.server.model.Permission.{ KeyPair, Vendor }
import com.snowplowanalytics.iglu.server.model.SchemaDraft.DraftId

// I was thinking how we can avoid something as powerful as Effect
// in signature, but came to conclusion that Storage will always
// be used in HttpService, where it is always be interpreted into Effect,
// so it won't make sense to add another F[_] -> E[_] transformation

trait Storage[F[_]] {

  def getSchemaWithMeta(schemaMap: SchemaMap)(implicit F: Effect[F]): F[Option[Schema]]
  def getPermission(apiKey: UUID)(implicit F: Effect[F]): F[Option[Permission]]
  def getSchemasByVendor(vendor: String, wildcard: Boolean)(implicit F: Effect[F]): F[List[Schema]] =
    getSchemas.map { schemas =>
      if (wildcard) schemas.filter(_.schemaMap.vendor.startsWith(vendor))
      else schemas.filter(_.schemaMap.vendor == vendor )
    }

  def getSchemasByVendorName(vendor: String, name: String)(implicit F: Effect[F]): F[List[Schema]] =
  getSchemasByVendor(vendor, true).map { schemas => schemas.filter(_.schemaMap.name == name) }

  def addSchema(schemaMap: SchemaMap, body: Json)(implicit F: Effect[F]): F[Unit]

  def getSchemas(implicit F: Effect[F]): F[List[Schema]]

  def getSchema(schemaMap: SchemaMap)(implicit F: Effect[F]): F[Option[Json]] =
    getSchemaWithMeta(schemaMap).nested.map(_.body).value

  def addDraft(draftId: DraftId, body: Json)(implicit F: Effect[F]): F[Unit]
  def getDraft(draftId: DraftId)(implicit F: Effect[F]): F[Option[SchemaDraft]]

  def addPermission(uuid: UUID, permission: Permission)(implicit F: Effect[F]): F[Unit]
  def deletePermission(uuid: UUID)(implicit F: Effect[F]): F[Unit]
  def addKeyPair(keyPair: KeyPair, vendor: Vendor)(implicit F: Effect[F]): F[Unit] =
    for {
      _ <- addPermission(keyPair.read, Permission.readOnly.copy(vendor = vendor))
      _ <- addPermission(keyPair.write, Permission.write.copy(vendor = vendor))
    } yield ()

  protected def getTime(implicit F: Sync[F]): F[Instant] =
    F.delay(Instant.now())
}

object Storage {
  def initialize[F[_]: Effect](config: StorageConfig): F[Storage[F]] = {
    config match {
      case StorageConfig.DummyConfig =>
        storage.InMemory.empty
      case StorageConfig.PostgresConfig(_, _, name, username, password) =>
        val driver = "org.postgresql.Driver"
        val url = s"jdbc:postgresql:$name"
        // TODO: throw exception if connection is not available
        val transactor = HikariTransactor.newHikariTransactor[F](driver, url, username, password)
        transactor.map(Postgres.apply)
    }
  }
}
