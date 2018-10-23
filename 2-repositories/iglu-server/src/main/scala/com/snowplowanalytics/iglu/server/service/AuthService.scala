package com.snowplowanalytics.iglu.server
package service

import java.util.UUID

import cats.implicits._
import cats.data.EitherT
import cats.effect.{ IO, Sync }

import io.circe._
import io.circe.syntax._

import org.http4s._
import org.http4s.circe._
import org.http4s.rho.{RhoMiddleware, RhoService}
import org.http4s.rho.swagger.syntax.{io => swaggerSyntax}

import com.snowplowanalytics.iglu.server.middleware.PermissionMiddleware
import com.snowplowanalytics.iglu.server.model.Permission
import com.snowplowanalytics.iglu.server.storage.Storage


class AuthService(db: Storage[IO]) extends RhoService[IO] {
  import swaggerSyntax._
  import AuthService._
  import PermissionMiddleware.Auth.auth


  val apikey = paramD[UUID]("key", "UUID apikey to delete")

  "Route to delete api key" **
    DELETE / "keygen" +? apikey >>> auth() |>> deleteKey _

  "Route to generate new keys" **
    POST / "keygen" >>> auth() |>> generateKey _


  def generateKey(req: Request[IO], authInfo: Permission) = {
    if (authInfo.key.contains(Permission.KeyAction.Create))
      for {
        vendor <- req.attemptAs[UrlForm]
          .subflatMap(vendorFromForm)
          .recoverWith { case MalformedMessageBodyFailure(_, None) => vendorFromBody(req) }
          .value.map(_.fold(throw _, identity))
        keyPair <- Permission.KeyPair.generate[IO]
        _ <- db.addKeyPair(keyPair, vendor)
        response <- Ok(keyPair.asJson)
      } yield response
    else Forbidden(Json.Null)
  }

  def deleteKey(key: UUID, permission: Permission) =
    if (permission.key.contains(Permission.KeyAction.Delete)) {
      db.deletePermission(key) *> Ok("boom!")
    } else Forbidden("Not enough power!")
}

object AuthService {

  case class GenerateKey(vendorPrefix: Permission.Vendor)

  implicit val schemaGenerateReq: Decoder[GenerateKey] =
    Decoder.instance { cursor =>
      cursor
        .downField("vendorPrefix")
        .as[String]
        .map(Permission.Vendor.parse)
        .map(GenerateKey.apply)
    }

  def asService(db: Storage[IO], rhoMiddleware: RhoMiddleware[IO]): HttpService[IO] = {
    val service = new AuthService(db).toService(rhoMiddleware)
    PermissionMiddleware.wrapService(db, service)
  }

  def vendorFromForm(urlForm: UrlForm): Either[DecodeFailure, Permission.Vendor] =
    urlForm
      .getFirst("vendor_prefix")
      .map(Permission.Vendor.parse)
      .toRight(InvalidMessageBodyFailure(s"Cannot extract vendor_prefix from ${UrlForm.encodeString(Charset.`UTF-8`)(urlForm)}"))

  def vendorFromBody[F[_]: Sync](request: Request[F]) = {
    request.attemptAs[Json].flatMap { json =>
      EitherT.fromEither[F](json.as[GenerateKey].fold(
        e => InvalidMessageBodyFailure(e.show).asLeft[Permission.Vendor],
        p => p.vendorPrefix.asRight[DecodeFailure])
      )
    }
  }
}
