package com.xebia.data.spot

import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.trace.Tracer

/**
 * Grants access to an OpenTelemetry instance.
 *
 * If no configuration is provided, this attempts to load the spot.autoconf.SdkProvider, which is defined in the "spot-
 * complete" subproject. If the configuration contains a value for the key 'com.xebia.data.spot.sdkProvider', it
 * attempts to load the class indicated by that value.
 */
trait OpenTelemetrySupport {
  def spotConfig: Map[String, String]

  val openTelemetry: OpenTelemetry = {
    val provFQCN = spotConfig.getOrElse("com.xebia.data.spot.sdkProvider", "com.xebia.data.spot.autoconf.SdkProvider")
    val provClass = java.lang.Class.forName(provFQCN)
    val provider = provClass.getDeclaredConstructor().newInstance().asInstanceOf[OpenTelemetrySdkProvider]
    provider.get(spotConfig)
  }

  lazy val tracer: Tracer = ???
}
