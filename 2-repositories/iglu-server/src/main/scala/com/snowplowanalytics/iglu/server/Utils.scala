package com.snowplowanalytics.iglu.server

import org.json4s.JValue
import org.json4s.jackson.JsonMethods.{ compact, parse => parseJson4s }

import io.circe.Json
import io.circe.jawn.parse

import com.snowplowanalytics.iglu.core.{SchemaKey, SchemaMap, SchemaVer}
import com.snowplowanalytics.iglu.schemaddl.jsonschema.Schema
import com.snowplowanalytics.iglu.schemaddl.jsonschema.json4s.implicits._


object Utils {

  implicit class DowngradeJson(json: Json) {
    def fromCirce: JValue = parseJson4s(json.noSpaces)
  }

  implicit class UpgradeJson(json: JValue) {
    def toCirce: Json =
      parse(compact(json))
        .getOrElse(throw new RuntimeException("Unexpected JSON conversion exception"))
  }

  implicit class UpgradeSchema(schema: Schema) {
    def toCirce: Json =
      parse(compact(Schema.normalize(schema)))
        .getOrElse(throw new RuntimeException("Unexpected JSON conversion exception"))
  }

  object FormatPath {
    def unapply(arg: String): Option[String] =
      arg match {
        case "jsonschema" => Some(arg)
        case _ => None
      }
  }

  def toSchemaMap(schemaKey: SchemaKey): SchemaMap =
    SchemaMap(schemaKey.vendor, schemaKey.name, schemaKey.format, schemaKey.version.asInstanceOf[SchemaVer.Full])
}
