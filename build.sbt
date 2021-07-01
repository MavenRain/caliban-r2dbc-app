val calibanVersion = "1.0.1"

lazy val root =
  project
    .in(file("."))
    .settings(
      organization := "io.github.mavenrain",
      name := "caliban-r2dbc-app",
      version := "0.1.0-SNAPSHOT",
      versionScheme := Some("early-semver"),
      scalaVersion := "3.0.0",
      // todo remove when fixed: https://github.com/lampepfl/dotty/issues/11943
      Compile / doc / sources := Seq(),
      libraryDependencies ++= Seq(
        "com.github.ghostdogpr" %% "caliban" % calibanVersion,
        "com.github.ghostdogpr" %% "caliban-zio-http" % calibanVersion,
        "dev.zio" %% "zio-interop-reactivestreams" % "1.3.5",
        "io.d11" %% "zhttp" % "1.0.0.0-RC17",
        "io.r2dbc" % "r2dbc-h2" % "0.8.4.RELEASE"
      )
    )

assembly / assemblyMergeStrategy := {
  case PathList("META-INF", "MANIFEST.MF") => MergeStrategy.discard
  case _ => MergeStrategy.first
}
dockerExposedPorts += 9000
enablePlugins(JavaAppPackaging)
enablePlugins(DockerPlugin)
enablePlugins(CodegenPlugin)