/*
 * Copyright 2024 Xebia Netherlands
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
    "-Xsource:2.13.0",
)
ThisBuild / licenses += "Apache 2.0" -> url("https://apache.org/licenses/LICENSE-2.0")
ThisBuild / packageOptions += Package.ManifestAttributes(
  "License" -> "Apache-2.0",
  "License-URL" -> "http://www.apache.org/licenses/LICENSE-2.0"
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
        // The spot-complete subproject has no purpose as a stand-alone JAR file, we only care about the assembly.
        Compile / packageBin / artifactName := { (_, _, _) => "_ignore-me.jar" },
        Compile / packageBin / publishArtifact := false,
        // The scala library is shipped with Spark, we don't need our own copy.
        assembly / assemblyOption ~= {
            _.withIncludeScala(false)
        },
        assembly / assemblyMergeStrategy := {
            // We don't need the module files, easiest way out of the name class is to leave them out.
            case "META-INF/versions/9/module-info.class" => MergeStrategy.discard
            case "META-INF/okio.kotlin_module" => MergeStrategy.discard
            case x =>
                val oldStrategy = (ThisBuild / assemblyMergeStrategy).value
                oldStrategy(x)
        },
        assembly / assemblyShadeRules := Seq(
            // The opentelemetry gRPC sender relies on OkHttp. It also exists somewhere else in a Spark classpath, and
            // we've seen classpath version issues, specifically NoSuchMethodError. We use shading as a workaround,
            // because then we don't have to make any assumptions about our target environment.
            ShadeRule.rename("okhttp3.**" -> "spot.shaded.okhttp3.@1", "okio.**" -> "spot.shaded.okio.@1").inAll
        ),
    )

lazy val root = projectMatrix
    .withId("spark-opentelemetry")
    .in(file("."))
    .aggregate(spot, `spot-complete`)
    .disablePlugins(AssemblyPlugin)
    .settings(
        publish / skip := true,
    )
