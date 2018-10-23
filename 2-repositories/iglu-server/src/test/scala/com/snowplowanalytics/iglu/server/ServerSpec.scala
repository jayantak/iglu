package com.snowplowanalytics.iglu.server

import cats.effect.IO

import org.http4s._
import org.http4s.rho.swagger.syntax.io.createRhoMiddleware

import com.snowplowanalytics.iglu.server.service.SchemaService

class ServerSpec extends org.specs2.Specification { def is = s2"""
  Return 200 for public schema $e1
  Return empty JSON object for public schema $e2
  """

  def e1 = {
    val response = ServerSpec.request(Request(Method.GET, Uri.uri("/com.acme/event/jsonschema/1-0-0")))
    response.unsafeRunSync().status must beEqualTo(Status.Ok)
  }

  def e2 = {
    val response = ServerSpec.request(Request(Method.GET, Uri.uri("/com.acme/event/jsonschema/1-0-0")))
    response.flatMap(_.as[String]).unsafeRunSync() must beEqualTo("{}")
  }
}

object ServerSpec {
  val middleware = createRhoMiddleware()

  def request(req: Request[IO]): IO[Response[IO]] = {
    for {
      storage <- storage.InMemory.get[IO](SpecHelpers.exampleState)
      service <- SchemaService.asService(storage, middleware).run(req).value
    } yield service.getOrElse(Response(Status.NotFound))
  }
}
