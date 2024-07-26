package com.xebia.data.spot

import io.opentelemetry.api.OpenTelemetry

/**
 * Enables spot to obtain an OpenTelemetry SDK instance.
 */
trait OpenTelemetrySdkProvider {
  /**
   * Returns an instance of [[OpenTelemetry]].
   *
   * @param config all SparkConf values.
   * @return an instance of [[OpenTelemetry]].
   */
  def get(config: Map[String, String]): OpenTelemetry
}
