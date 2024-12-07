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
import sbt._

object Dependencies {
  private[this] val openTelemetryVersion = "1.45.0"

  val `opentelemetry-api` = "io.opentelemetry" % "opentelemetry-api" % openTelemetryVersion
  val `opentelemetry-sdk` = "io.opentelemetry" % "opentelemetry-sdk" % openTelemetryVersion
  val `opentelemetry-sdk-autoconfigure` = "io.opentelemetry" % "opentelemetry-sdk-extension-autoconfigure" % openTelemetryVersion
  val `opentelemetry-sdk-testing` = "io.opentelemetry" % "opentelemetry-sdk-testing" % openTelemetryVersion
  val `opentelemetry-exporter-otlp` = "io.opentelemetry" % "opentelemetry-exporter-otlp" % openTelemetryVersion
  val `spark-core-3.3` = "org.apache.spark" %% "spark-core" % "3.3.4"
  val `spark-core-3.4` = "org.apache.spark" %% "spark-core" % "3.4.4"
  val `spark-core-3.5` = "org.apache.spark" %% "spark-core" % "3.5.3"
  val scalactic = "org.scalactic" %% "scalactic" % "3.2.19"
  val scalaTest = "org.scalatest" %% "scalatest" % "3.2.19"
  val `assertj-core` = "org.assertj" % "assertj-core" % "3.26.3"
}
