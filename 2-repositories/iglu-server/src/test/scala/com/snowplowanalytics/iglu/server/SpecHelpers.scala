package com.snowplowanalytics.iglu.server

import java.util.UUID
import java.time.Instant

import cats.implicits._
import cats.effect.IO

import io.circe.Json

import org.http4s._

import com.snowplowanalytics.iglu.core.{ SchemaMap, SchemaVer }

import model.Permission
import model.Schema
import storage.{ InMemory, Storage }


object SpecHelpers {
  val now = Instant.ofEpochMilli(1537621061000L)
  val masterKey = UUID.fromString("4ed2d87a-6da5-48e8-a23b-36a26e61f974")
  val readKey = UUID.fromString("1eaad173-1da5-eef8-a2cb-3fa26e61f975")

  val schemas = Map(
    SchemaMap("com.acme", "event", "jsonschema", SchemaVer.Full(1,0,0)) ->
      Schema(SchemaMap("com.acme", "event", "jsonschema", SchemaVer.Full(1,0,0)), Schema.Metadata(now, now, true), Json.fromFields(Nil)),
    SchemaMap("com.acme", "event", "jsonschema", SchemaVer.Full(1,0,1)) ->
      Schema(SchemaMap("com.acme", "event", "jsonschema", SchemaVer.Full(1,0,1)), Schema.Metadata(now, now, false), Json.fromFields(Nil)))

  val exampleState = InMemory.State(schemas, Map(masterKey -> Permission.master, readKey -> Permission.readOnly))

  /** Run multiple requests against HTTP service and return all responses and result state */
  def state(ser: Storage[IO] => HttpService[IO])(reqs: List[Request[IO]]): IO[(List[Response[IO]], InMemory.State)] = {
    for {
      storage <- InMemory.getInMemory[IO](exampleState)
      service = ser(storage)
      responses <- reqs.traverse(service.run).value
      state <- storage.ref.get
    } yield (responses.getOrElse(List.empty), state)
  }
}
