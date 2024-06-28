import sbt._

object Dependencies {
  private[this] val openTelemetryVersion = "1.39.0"
  private[this] val openTelemetryAutoConf = "1.38.0"

  val `opentelemetry-api` = "io.opentelemetry" % "opentelemetry-api" % openTelemetryVersion
  val `opentelemetry-sdk` = "io.opentelemetry" % "opentelemetry-sdk" % openTelemetryVersion
  val `opentelemetry-sdk-autoconfigure` = "io.opentelemetry" % "opentelemetry-sdk-extension-autoconfigure" % openTelemetryAutoConf
  val `spark-core` = "org.apache.spark" %% "spark-core" % "3.5.1"
}
