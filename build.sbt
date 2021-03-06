name := "Typed Schema"

moduleName := "typed-schema-all"

lazy val compilerPlugins = List(
  addCompilerPlugin("org.typelevel"   %% "kind-projector"     % Version.kindProjector),
  addCompilerPlugin("org.scalamacros" % "paradise"            % "2.1.1" cross CrossVersion.patch),
  addCompilerPlugin("com.olegpy"      %% "better-monadic-for" % "0.3.1")
)

val catsCore        = "org.typelevel"        %% s"cats-core"        % Version.cats
val catsFree        = "org.typelevel"        %% s"cats-free"        % Version.cats
val catsEffect      = "org.typelevel"        %% s"cats-effect"      % Version.catsEffect
val simulacrum      = "com.github.mpilquist" %% "simulacrum"        % Version.simulacrum
val shapeless       = "com.chuusai"          %% "shapeless"         % Version.shapeless
val enumeratum      = "com.beachape"         %% "enumeratum"        % Version.enumeratum
val magnolia        = "com.propensive"       %% "magnolia"          % Version.magnolia
val akkaHttpLib     = "com.typesafe.akka"    %% "akka-http"         % Version.akkaHttp
val scalatest       = "org.scalatest"        %% "scalatest"         % Version.scalaTest % Test
val scalacheck      = "org.scalacheck"       %% "scalacheck"        % Version.scalaCheck % Test
val akkaTestKit     = "com.typesafe.akka"    %% "akka-testkit"      % Version.akka % Test
val akkaHttpTestKit = "com.typesafe.akka"    %% "akka-http-testkit" % Version.akkaHttp % Test
val akkaHttpCirce   = "de.heikoseeberger"    %% "akka-http-circe"   % Version.akkaHttpCirce % Test
val finagleHttp     = "com.twitter"          %% "finagle-http"      % Version.finagle
val circeDerivation = "io.circe"             %% "circe-derivation"  % Version.circeDerivation
val scalazDeriving  = "org.scalaz"           %% "scalaz-deriving"   % Version.scalazDeriving
val scalazDMacro    = "org.scalaz"           %% "deriving-macro"    % Version.scalazDeriving
val derevo          = "org.manatki"          %% "derevo-cats"       % Version.derevo
val swaggerUILib    = "org.webjars.npm"      % "swagger-ui-dist"    % Version.swaggerUI
val scalaTags       = "com.lihaoyi"          %% "scalatags"         % Version.scalaTags

val monocle = List("core", "macro").map(module => "com.github.julien-truffaut" %% s"monocle-$module" % Version.monocle)
val circe   = List("core", "parser", "generic", "generic-extras").map(module => "io.circe" %% s"circe-$module" % Version.circe)
val akka    = List("actor", "stream").map(module => "com.typesafe.akka" %% s"akka-$module" % Version.akka)
val zio     = List("dev.zio" %% "zio" % Version.zio, "dev.zio" %% "zio-interop-cats" % Version.zioCats)
val tethys  = List("core", "jackson").map(module => "com.tethys-json" %% s"tethys-$module" % Version.tethys)

val reflect  = libraryDependencies += scalaOrganization.value % "scala-reflect"  % scalaVersion.value
val compiler = libraryDependencies += scalaOrganization.value % "scala-compiler" % scalaVersion.value

val enumeratumCirce = "com.beachape" %% "enumeratum-circe" % Version.enumeratumCirce

def resourcesOnCompilerCp(config: Configuration): Setting[_] =
  managedClasspath in config := {
    val res = (resourceDirectory in config).value
    val old = (managedClasspath in config).value
    Attributed.blank(res) +: old
  }

val swaggerUIVersion = SettingKey[String]("swaggerUIVersion")

lazy val testLibs = libraryDependencies ++= scalacheck :: scalatest :: Nil

lazy val commonSettings = testLibs :: compilerPlugins

lazy val kernel = project
  .in(file("modules/kernel"))
  .settings(
    commonSettings,
    moduleName := "typed-schema-typedsl",
    libraryDependencies ++= catsCore :: simulacrum :: shapeless :: enumeratum :: magnolia :: monocle
  )

lazy val param = project
  .in(file("modules/param"))
  .dependsOn(kernel)
  .settings(
    commonSettings,
    moduleName := "typed-schema-param"
  )

lazy val macros = project
  .in(file("modules/macros"))
  .dependsOn(kernel)
  .settings(
    commonSettings,
    moduleName := "typed-schema-macros",
    libraryDependencies ++= shapeless :: catsCore :: akkaHttpTestKit :: Nil,
    reflect
  )

lazy val swagger = project
  .in(file("modules/swagger"))
  .dependsOn(kernel, macros)
  .settings(
    commonSettings,
    moduleName := "typed-schema-swagger",
    libraryDependencies ++= akka ::: akkaHttpLib :: enumeratum :: enumeratumCirce :: circeDerivation :: circe
  )

lazy val akkaHttp = project
  .in(file("modules/akkaHttp"))
  .dependsOn(kernel, macros, param)
  .settings(
    commonSettings,
    moduleName := "typed-schema-akka-http",
    libraryDependencies ++= akkaHttpLib :: akka
  )

lazy val finagle = project
  .in(file("modules/finagle"))
  .dependsOn(kernel, macros, param)
  .settings(
    commonSettings,
    moduleName := "typed-schema-finagle",
    libraryDependencies ++= finagleHttp :: catsEffect :: catsFree :: Nil
  )

lazy val finagleCirce = project
  .in(file("modules/finagleCirce"))
  .dependsOn(finagle)
  .settings(
    commonSettings,
    moduleName := "typed-schema-finagle-circe",
    libraryDependencies ++= circe
  )

lazy val finagleTethys = project
  .in(file("modules/finagleTethys"))
  .dependsOn(finagle)
  .settings(
    commonSettings,
    moduleName := "typed-schema-finagle-tethys",
    libraryDependencies ++= tethys
  )

lazy val finagleZio = project
  .in(file("modules/finagle-zio"))
  .dependsOn(finagle)
  .settings(
    commonSettings,
    moduleName := "typed-schema-finagle-zio",
    libraryDependencies ++= catsEffect :: zio
  )

lazy val main = project
  .in(file("modules/main"))
  .dependsOn(kernel, macros, swagger, akkaHttp)
  .settings(
    commonSettings,
    moduleName := "typed-schema",
    libraryDependencies ++= akkaHttpCirce :: akkaHttpLib :: akkaHttpTestKit :: akkaTestKit :: magnolia :: akka
  )

lazy val scalaz = project
  .in(file("modules/scalaz"))
  .dependsOn(main)
  .settings(
    commonSettings,
    moduleName := "typed-schema-scalaz",
    libraryDependencies ++= scalazDeriving :: scalazDMacro :: Nil,
    addCompilerPlugin("org.scalaz" %% "deriving-plugin" % Version.scalazDeriving),
    resourcesOnCompilerCp(Compile)
  )

lazy val swaggerUI =
  (project in file("modules/swaggerUI"))
    .dependsOn(swagger)
    .enablePlugins(BuildInfoPlugin)
    .settings(
      commonSettings,
      moduleName := "typed-schema-swagger-ui",
      libraryDependencies ++= swaggerUILib :: scalaTags :: Nil,
      swaggerUIVersion := {
        libraryDependencies.value
          .find(_.name == "swagger-ui-dist")
          .map(_.revision)
          .get
      },
      buildInfoKeys := swaggerUIVersion :: Nil,
      buildInfoPackage := "ru.tinkoff.tschema.swagger"
    )

lazy val docs = project.in(file("modules/docs"))
  .enablePlugins(ScalaUnidocPlugin)
  .settings(Seq(
    unidocProjectFilter in (ScalaUnidoc, unidoc) := inProjects(main, kernel, swagger, akkaHttp)
  ))
  .dependsOn(kernel, macros, main, akkaHttp)
  .settings(commonSettings)

lazy val typedschema =
  (project in file("."))
    .dependsOn(macros, kernel, main)
    .aggregate(macros,
               kernel,
               main,
               param,
               swagger,
               akkaHttp,
               scalaz,
               finagle,
               finagleZio,
               finagleCirce,
               finagleTethys,
               swaggerUI,
               docs)
