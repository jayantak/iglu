package com.snowplowanalytics.iglu.server.model

import java.util.UUID

import cats.Order
import cats.instances.int._
import cats.effect.Sync

import io.circe.Encoder
import io.circe.generic.semiauto._

import Permission._

// Key A with x.y.z vendor CANNOT create key B with x.y vendor
// Key A with any non-empty KeyAction set always has read key-permissions

case class Permission(vendor: Vendor, schema: Option[SchemaAction], key: Set[KeyAction]) {
  /** Check if user has enough rights to read particular schema */
  def canRead(schemaVendor: String): Boolean =
    this match {
      case Permission(_, Some(_), _) =>
        Vendor.check(schemaVendor, vendor)
      case Permission(_, _, keyActions) if keyActions.nonEmpty =>
        Vendor.check(schemaVendor, vendor)
      case _ => false
    }

  /** Check if user has enough rights to create particular schema */
  def canCreate(schemaVendor: String): Boolean =
    this match {
      case Permission(_, Some(action), _) if action != SchemaAction.Read =>
        Vendor.check(schemaVendor, vendor)
      case Permission(_, _, keyActions) if keyActions.nonEmpty =>
        Vendor.check(schemaVendor, vendor)
      case _ => false
    }
}

object Permission {

  case class KeyPair(read: UUID, write: UUID)

  object KeyPair {
    def generate[F[_]](implicit F: Sync[F]): F[KeyPair] =
      F.delay(KeyPair(UUID.randomUUID(), UUID.randomUUID()))

    implicit val encoder: Encoder[KeyPair] =
      deriveEncoder[KeyPair]
  }

  /**
    * Permission regarding vendor
    * @param parts dot-separated namespace, where permission can be applied
    * @param wildcard whether permission applied to any "smaller" vendor
    *                 or just specified in `parts`
    */
  case class Vendor(parts: List[String], wildcard: Boolean)
  object Vendor {
    /** Can be applied to any vendor */
    val wildcard = Vendor(Nil, true)

    /** Cannot be applied to any vendor */
    val noop = Vendor(Nil, false)

    def check(schemaVendor: String, vendor: Vendor) = {
      val original = schemaVendor.split(".").toList
      if (vendor == wildcard) true
      else if (original == vendor.parts) true
      else if (original.take(vendor.parts.length) == vendor.parts && vendor.wildcard) true
      else false
    }

    def parse(string: String): Vendor =
      string match {
        case "*" => wildcard
        case vendor => Vendor(vendor.split('.').toList, true)
      }
  }

  sealed trait SchemaAction extends Product with Serializable
  object SchemaAction {
    /** Only get/view schemas */
    case object Read extends SchemaAction
    /** Bump schema versions within existing schema and read */
    case object Bump extends SchemaAction
    /** Create new schemas/names (but within attached vendor permission) */
    case object Create extends SchemaAction
    /** Do everything, including creating new "subvendor" (applied only for `Vendor` with `wildcard`) */
    case object CreateVendor extends SchemaAction

    val all: List[SchemaAction] =
      List(Read, Bump, Create, CreateVendor)

    implicit val ordering: Order[SchemaAction] =
      Order.by[SchemaAction, Int](all.zipWithIndex.toMap)
  }

  sealed trait KeyAction extends Product with Serializable
  object KeyAction {
    case object Create extends KeyAction
    case object Delete extends KeyAction

    val all: Set[KeyAction] = Set(Create, Delete)
  }
  /** Anonymous user, authorized for nothing */
  val noop = Permission(Vendor.noop, None, Set.empty)

  /** Admin permission, allowed to create any schemas and keys */
  val master = Permission(Vendor.wildcard, Some(SchemaAction.CreateVendor), KeyAction.all)

  /** Read any schema */
  val readOnly = Permission(Vendor.wildcard, Some(SchemaAction.Read), Set.empty)

  /** Read, write and create any schemas, but nothing for keys */
  val write = Permission(Vendor.wildcard, Some(SchemaAction.CreateVendor), Set.empty)
}