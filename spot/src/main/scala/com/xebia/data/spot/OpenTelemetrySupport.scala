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
