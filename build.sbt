ThisBuild / organization := "com.xebia.data"
ThisBuild / description := "OpenTelemetry Tracing for Apache Spark applications"
ThisBuild / homepage := Some(url("https://github.com/godatadriven/spot"))
ThisBuild / scmInfo := Some(ScmInfo(
        url("https://github.com/godatadriven/spot"),
        "https://github.com/godatadriven/spot.git",
        "git@github.com:godatadriven/spot.git"))

ThisBuild / scalaVersion := "2.13.15"
ThisBuild / scalacOptions := Seq(
    "--deprecation",
    "--release:11",
)

import Dependencies._

lazy val scalaVersions = Seq("2.12.20", "2.13.15")
lazy val Spark3_3 = SparkVersionAxis("-3_3", "spark-3.3")
lazy val Spark3_4 = SparkVersionAxis("-3_4", "spark-3.4")
lazy val Spark3_5 = SparkVersionAxis("-3_5", "spark-3.5")

lazy val spot = projectMatrix
    .in(file("./spot"))
    .customRow(
        scalaVersions=scalaVersions,
        axisValues=Spark3_3.axisValues,
        _.settings(
            moduleName := name.value + "-3.3",
            libraryDependencies += `spark-core-3.3` % Provided
        )
    )
    .customRow(
        scalaVersions=scalaVersions,
        axisValues=Spark3_4.axisValues,
        _.settings(
            moduleName := name.value + "-3.4",
            libraryDependencies += `spark-core-3.4` % Provided
        )
    )
    .customRow(
        scalaVersions=scalaVersions,
        axisValues=Spark3_5.axisValues,
        _.settings(
            moduleName := name.value + "-3.5",
            libraryDependencies += `spark-core-3.5` % Provided
        )
    )
    .disablePlugins(AssemblyPlugin)
    .settings(
        name := "spot",
        libraryDependencies ++= Seq(
            `opentelemetry-api`,
            scalaTest % Test,
            scalactic % Test,
            `opentelemetry-sdk-testing` % Test,
            `assertj-core` % Test,
        ),
    )

lazy val `spot-complete` = projectMatrix
    .in(file("./spot-complete"))
    .customRow(
        scalaVersions=scalaVersions,
        axisValues=Spark3_3.axisValues,
        _.settings(
            assembly / assemblyJarName := s"${name.value}-3.3_${scalaBinaryVersion.value}-${version.value}.jar",
            libraryDependencies += `spark-core-3.3` % Provided
        )
    )
    .customRow(
        scalaVersions=scalaVersions,
        axisValues=Spark3_4.axisValues,
        _.settings(
            assembly / assemblyJarName := s"${name.value}-3.4_${scalaBinaryVersion.value}-${version.value}.jar",
            libraryDependencies += `spark-core-3.4` % Provided
        )
    )
    .customRow(
        scalaVersions=scalaVersions,
        axisValues=Spark3_5.axisValues,
        _.settings(
            assembly / assemblyJarName := s"${name.value}-3.5_${scalaBinaryVersion.value}-${version.value}.jar",
            libraryDependencies += `spark-core-3.5` % Provided
        )
    )
    .dependsOn(spot)
    .settings(
        name := "spot-complete",
        libraryDependencies ++= Seq(
            `opentelemetry-sdk`,
            `opentelemetry-sdk-autoconfigure`,
            `opentelemetry-exporter-otlp`,
            scalaTest % Test,
            scalactic % Test
        ),
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

lazy val root = projectMatrix
    .withId("spark-opentelemetry")
    .in(file("."))
    .aggregate(spot, `spot-complete`)
    .disablePlugins(AssemblyPlugin)
    .settings(
        publish / skip := true,
    )
