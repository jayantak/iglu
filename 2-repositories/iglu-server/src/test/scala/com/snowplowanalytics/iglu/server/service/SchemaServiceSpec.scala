package com.snowplowanalytics.iglu.server
package service

import fs2.Stream

import io.circe.literal._

import cats.implicits._
import cats.effect.IO

import org.http4s._
import org.http4s.rho.swagger.syntax.io.createRhoMiddleware

import com.snowplowanalytics.iglu.core.{SchemaMap, SchemaVer}


class SchemaServiceSpec extends org.specs2.Specification { def is = s2"""
  Returns 404 on non-existing schema $e1
  PUT request adds schema $e2
  """

  def e1 = {
    val req: Request[IO] =
      Request(Method.GET, Uri.uri("/com.acme/nonexistent/jsonschema/1-0-0"))

    val response = SchemaServiceSpec.request(List(req))
    response.unsafeRunSync().status must beEqualTo(Status.NotFound)
  }

  def e2 = {
    val selfDescribingSchema =
      json"""
        {
          "self": {
            "vendor": "com.acme",
            "name": "nonexistent",
            "format": "jsonschema",
            "version": "1-0-0"
          },
          "type": "object"
        }"""
    val exmapleSchema = Stream.emits(selfDescribingSchema.noSpaces.stripMargin.getBytes).covary[IO]

    val reqs: List[Request[IO]] = List(
      Request[IO](Method.PUT, Uri.uri("/com.acme/nonexistent/jsonschema/1-0-0"))
        .withContentType(headers.`Content-Type`(MediaType.`application/json`))
        .withHeaders(Headers(Header("apikey", "4ed2d87a-6da5-48e8-a23b-36a26e61f974")))
        .withBodyStream(exmapleSchema),
      Request[IO](Method.GET, Uri.uri("/com.acme/nonexistent/jsonschema/1-0-0"))
    )

    val (requests, state) = SchemaServiceSpec.state(reqs).unsafeRunSync()
    val dbExpectation = state.schemas.mapValues(s => (s.metadata.isPublic, s.body)) must havePair(
      (SchemaMap("com.acme", "nonexistent", "jsonschema", SchemaVer.Full(1,0,0)), (true, json"""{"type": "object"}"""))
    )
    val requestExpectation = requests.lastOption.map(_.status) must beSome(Status.Ok)
    dbExpectation and requestExpectation
  }
}

object SchemaServiceSpec {

  import storage.InMemory

  def request(reqs: List[Request[IO]]): IO[Response[IO]] = {
    for {
      storage <- InMemory.getInMemory[IO](SpecHelpers.exampleState)
      service = SchemaService.asService(storage, createRhoMiddleware())
      responses <- reqs.traverse(service.run).value
    } yield responses.flatMap(_.lastOption).getOrElse(Response(Status.NotFound))
  }

  def state(reqs: List[Request[IO]]): IO[(List[Response[IO]], InMemory.State)] = {
    for {
      storage <- InMemory.getInMemory[IO](SpecHelpers.exampleState)
      service = SchemaService.asService(storage, createRhoMiddleware())
      responses <- reqs.traverse(service.run).value
      state <- storage.ref.get
    } yield (responses.getOrElse(List.empty), state)
  }
}
