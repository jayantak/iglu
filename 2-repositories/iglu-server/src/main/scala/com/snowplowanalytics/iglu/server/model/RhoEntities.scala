package com.snowplowanalytics.iglu.server.model

import scala.reflect.runtime.universe._

import cats.Monad

import eu.timepit.refined.types.numeric.NonNegInt

import org.http4s.rho.bits._

import com.snowplowanalytics.iglu.core.SchemaVer

object RhoEntities {
  implicit def schemaVerParser[F[_]]: StringParser[F, SchemaVer.Full] =
    new StringParser[F, SchemaVer.Full] {
      override val typeTag: Some[TypeTag[SchemaVer.Full]] = Some(implicitly[TypeTag[SchemaVer.Full]])

      override def parse(s: String)(implicit F: Monad[F]): ResultResponse[F, SchemaVer.Full] =
        SchemaVer.parseFull(s) match {
          case Some(v) => SuccessResponse(v)
          case None => FailureResponse.pure[F](BadRequest.pure(s"Invalid boolean format: '$s'"))
        }
    }

  implicit def draftVersionParser[F[_]]: StringParser[F, DraftVersion] =
    new StringParser[F, DraftVersion] {
      override val typeTag: Some[TypeTag[DraftVersion]] = Some(implicitly[TypeTag[DraftVersion]])

      override def parse(s: String)(implicit F: Monad[F]): ResultResponse[F, DraftVersion] = {
        val int = try { Right(s.toInt) } catch { case _: NumberFormatException => Left(s"$s is not an integer") }
        int.flatMap(NonNegInt.from).fold(err => FailureResponse.pure[F](BadRequest.pure(err)), SuccessResponse.apply)
      }
    }
}
