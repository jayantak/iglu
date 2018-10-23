package com.snowplowanalytics.iglu.server.service

import cats.effect.IO
import org.http4s.{HttpService, Request, Response, StaticFile}
import org.http4s.dsl.io._

object StaticContentService {
  private val localUi = "/swagger-ui-dist"
  private val swaggerUiDir = "/META-INF/resources/webjars/swagger-ui/3.19.0"

  def fetchResource(path: String, req: Request[IO]): IO[Response[IO]] = {
    StaticFile.fromResource(path, Some(req)).getOrElseF(NotFound())
  }
  /**
    * Routes for getting static resources. These might be served more efficiently by apache2 or nginx,
    * but its nice to keep it self contained
    */
  def routes: HttpService[IO] = HttpService {
    // Swagger User Interface
    case req @ GET -> Root / "swagger-ui" / "index.html" => fetchResource(localUi + "/index.html", req)
    case req @ GET -> Root / "swagger-ui" / path         => fetchResource(swaggerUiDir + "/" + path, req)
  }
}

