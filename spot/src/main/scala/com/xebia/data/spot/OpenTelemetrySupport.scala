package com.xebia.data.spot

import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.trace.Tracer
import org.apache.spark.SparkConf

private[spot] trait OpenTelemetrySupport {
  def conf: OpenTelemetryConfig = ???
  lazy val tracer: Tracer = ???
}

private[spot] case class OpenTelemetryConfig() {

}

private[spot] object OpenTelemetryConfig {
  def from(conf: SparkConf): OpenTelemetryConfig = ???
}
