package com.snowplowanalytics.iglu.server.middleware

import java.util.UUID

import cats.MonadError
import cats.data.{Kleisli, OptionT}
import cats.effect.{Effect, IO}
import cats.implicits._

import org.http4s.{ Request, HttpService }
import org.http4s.server.AuthMiddleware
import org.http4s.util.CaseInsensitiveString
import org.http4s.rho.AuthedContext

import com.snowplowanalytics.iglu.server.model.Permission
import com.snowplowanalytics.iglu.server.storage.Storage


/** Used only in HTTP Services, where all endpoints require authentication */
object PermissionMiddleware {

  val ApiKey = "apikey"

  /** Rho middleware */
  object Auth extends AuthedContext[IO, Permission]

  /** Build an authentication middleware on top of storage */
  def apply[F[_]: Effect](storage: Storage[F]): AuthMiddleware[F, Permission] =
    AuthMiddleware(Kleisli { request => auth[F](storage)(request) })

  /** Authenticate request against storage */
  def auth[F[_]: Effect](storage: Storage[F])(request: Request[F]): OptionT[F, Permission] = {
    val F = MonadError[F, Throwable]
    getApiKey(request).map(_.fold(F.raiseError[UUID], F.pure)) match {
      case Some(apiKey) =>
        OptionT(apiKey.flatMap(storage.getPermission))
      case None =>
        OptionT.pure(Permission.noop)
    }
  }

  /** Extract API key from HTTP request */
  def getApiKey[F[_]](request: Request[F]): Option[Either[Throwable, UUID]] =
    request.headers.get(CaseInsensitiveString(ApiKey))
      .map { header => header.value }
      .map { apiKey => Either.catchNonFatal(UUID.fromString(apiKey)) }

  def wrapService(db: Storage[IO], service: HttpService[IO]): HttpService[IO] =
    PermissionMiddleware[IO](db).apply(Auth.toService(service))
}
