import sbt._

object Dependencies {
  private[this] val openTelemetryVersion = "1.40.0"

  val `opentelemetry-api` = "io.opentelemetry" % "opentelemetry-api" % openTelemetryVersion
  val `opentelemetry-sdk` = "io.opentelemetry" % "opentelemetry-sdk" % openTelemetryVersion
  val `opentelemetry-sdk-autoconfigure` = "io.opentelemetry" % "opentelemetry-sdk-extension-autoconfigure" % openTelemetryVersion
  val `opentelemetry-exporter-otlp` = "io.opentelemetry" % "opentelemetry-exporter-otlp" % openTelemetryVersion
  val `spark-core` = "org.apache.spark" %% "spark-core" % "3.5.1"
  val scalactic = "org.scalactic" %% "scalactic" % "3.2.19"
  val scalaTest = "org.scalatest" %% "scalatest" % "3.2.19"
}
