package com.xebia.data.spot

import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.trace.Tracer
import org.slf4j.{Logger, LoggerFactory}

/** Grants access to an OpenTelemetry instance.
  *
  * If no configuration is provided, this attempts to load the spot.autoconf.SdkProvider, which is defined in the "spot-
  * complete" subproject. If the configuration contains a value for the key 'spark.com.xebia.data.spot.sdkProvider', it
  * attempts to load the class indicated by that value.
  */
trait OpenTelemetrySupport {
  import OpenTelemetrySupport.logger

  def spotConfig: Map[String, String]

  val openTelemetry: OpenTelemetry = {
    val provFQCN = spotConfig.getOrElse(configPrefixed("sdkProvider"), DEFAULT_PROVIDER_FQCN)
    logger.info(s"Using OpenTelemetrySdkProvider FQCN: '$provFQCN'")
    val provClass = java.lang.Class.forName(provFQCN)
    val provider = provClass.getDeclaredConstructor().newInstance().asInstanceOf[OpenTelemetrySdkProvider]
    val otel = provider.get(spotConfig)
    logger.info(s"OpenTelemetry SDK found: $otel")
    otel
  }

  lazy val tracer: Tracer = openTelemetry.getTracer("spot", version())
}

object OpenTelemetrySupport {
  @transient
  protected lazy val logger: Logger = LoggerFactory.getLogger(classOf[OpenTelemetrySupport])
}
