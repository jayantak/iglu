import sbt._

object Dependencies {

  object V {
    val IgluCore   = "0.3.0"
    val SchemaDdl  = "0.8.0"

    val Http4s     = "0.18.19"
    val Rho        = "0.18.0"
    val Doobie     = "0.5.3"
    val Decline    = "0.5.0"
    val Cats       = "1.4.0"
    val CatsEffect = "1.0.0-RC2"
    val Circe      = "0.9.3"
    val Refined    = "0.9.2"
    val SwaggerUi  = "3.19.0"

    val Specs2     = "4.3.3"
    val Logback    = "1.2.3"
  }

  val all = Seq(
    "com.snowplowanalytics" %% "iglu-core-circe"     % V.IgluCore,
    "com.snowplowanalytics" %% "schema-ddl"          % V.SchemaDdl,

    "com.monovore"          %% "decline"             % V.Decline,
    "org.typelevel"         %% "cats-core"           % V.Cats,
    "org.http4s"            %% "http4s-blaze-server" % V.Http4s,
    "org.http4s"            %% "http4s-circe"        % V.Http4s,
    "org.http4s"            %% "http4s-dsl"          % V.Http4s,
    "org.http4s"            %% "rho-swagger"         % V.Rho,
    "io.circe"              %% "circe-generic"       % V.Circe,
    "io.circe"              %% "circe-java8"         % V.Circe,
    "io.circe"              %% "circe-literal"       % V.Circe,
    "io.circe"              %% "circe-refined"       % V.Circe,
    "org.tpolecat"          %% "doobie-core"         % V.Doobie,
    "org.tpolecat"          %% "doobie-postgres"     % V.Doobie,
    "org.tpolecat"          %% "doobie-hikari"       % V.Doobie,
    "eu.timepit"            %% "refined"             % V.Refined,

    "org.webjars"           %  "swagger-ui"          % V.SwaggerUi,
    "ch.qos.logback"        %  "logback-classic"     % V.Logback,
    "org.specs2"            %% "specs2-core"         % V.Specs2 % "test"
  )
}
