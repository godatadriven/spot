ThisBuild / organization := "com.xebia.data"
ThisBuild / homepage := Some(url("https://github.com/godatadriven/spot"))
ThisBuild / scmInfo := Some(ScmInfo(
        url("https://github.com/godatadriven/spot"),
        "https://github.com/godatadriven/spot.git",
        "git@github.com:godatadriven/spot.git"))

ThisBuild / scalaVersion := "2.13.15"
ThisBuild / crossScalaVersions := Seq("2.12.20", "2.13.15")
ThisBuild / scalacOptions := Seq(
    "--deprecation",
    "--release:11",
)

import Dependencies._

lazy val spot = project
    .in(file("./spot"))
    .disablePlugins(AssemblyPlugin)
    .settings(
        name := "spot",
        libraryDependencies ++= Seq(
            `opentelemetry-api`,
            `spark-core` % Provided,
            scalaTest % Test,
            scalactic % Test,
            `opentelemetry-sdk-testing` % Test,
            `assertj-core` % Test,
        ),
    )

lazy val `spot-complete` = project
    .in(file("./spot-complete"))
    .dependsOn(spot)
    .settings(
        name := "spot-complete",
        libraryDependencies ++= Seq(
            `opentelemetry-sdk`,
            `opentelemetry-sdk-autoconfigure`,
            `opentelemetry-exporter-otlp`,
            `spark-core` % Provided,
            scalaTest % Test,
            scalactic % Test
        ),
        assembly / assemblyJarName := s"${name.value}_${scalaBinaryVersion.value}-${version.value}.jar",
        assembly / assemblyOption ~= {
            _.withIncludeScala(false)
        },
        assembly / assemblyMergeStrategy := {
            // TODO this okio business may well be wrong. Revisit this once we have an integration test to run.
            case "META-INF/versions/9/module-info.class" => MergeStrategy.last
            case "META-INF/okio.kotlin_module" => MergeStrategy.last
            case x =>
                val oldStrategy = (ThisBuild / assemblyMergeStrategy).value
                oldStrategy(x)
        }
    )

lazy val root = project
    .withId("spark-opentelemetry")
    .in(file("."))
    .aggregate(spot, `spot-complete`)
    .disablePlugins(AssemblyPlugin)
    .settings(
        publish / skip := true,
    )
