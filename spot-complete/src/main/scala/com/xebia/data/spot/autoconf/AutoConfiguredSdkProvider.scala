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
package com.xebia.data.spot.autoconf

import com.xebia.data.spot.OpenTelemetrySdkProvider
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk
import org.slf4j.LoggerFactory

import java.util.concurrent.TimeUnit

/** Uses OpenTelemetry Autoconfigure to build an OpenTelemetry SDK.
  *
  * Any SparkConf properties that start with `spark.otel` (such as `spark.otel.service.name`) are exposed as JVM system
  * properties (sans `spark.` prefix). This allows otel configuration (see link below) to be included as `--conf` args
  * to spark-submit.
  *
  * To configure the autoconf SDK, see [[https://opentelemetry.io/docs/languages/java/configuration/]]. If you're on
  * Kubernetes, have a look at the OpenTelemetry Operator.
  */
class AutoConfiguredSdkProvider extends OpenTelemetrySdkProvider {
  private val logger = LoggerFactory.getLogger(classOf[AutoConfiguredSdkProvider])

  override def get(config: Map[String, String]): OpenTelemetrySdk = {
    logger.info("Using AutoConfigured OpenTelemetry SDK.")
    config.foreach {
      case (k, v) if k.startsWith("spark.otel") =>
        val otelProperty = k.substring(6)
        sys.props.get(otelProperty) match {
          case Some(old) =>
            logger.info(s"Replacing '$otelProperty' in JVM system properties, changing it from '$old' to '$v'.")
          case None =>
            logger.info(s"Adding '$otelProperty' to JVM system properties as '$v'.")
        }
        sys.props.put(otelProperty, v)
      case _ =>
    }
    val sdk = AutoConfiguredOpenTelemetrySdk.initialize().getOpenTelemetrySdk

    sys.addShutdownHook {
      logger.info("Allowing up to 1 minute for OpenTelemetry SDK to shut down.")
      val completion = sdk.shutdown()
      completion.whenComplete(() => {
        completion.getFailureThrowable match {
          case e: Throwable => logger.warn(s"OpenTelemetry SDK shut down with Exception: ${e.toString}", e)
          case _            => logger.info("OpenTelemetry SDK shut down successfully.")
        }
      })
      completion.join(1, TimeUnit.MINUTES)
      if (!completion.isDone) {
        logger.warn("OpenTelemetry SDK failed to shut down within timeout.")
      }
    }
    sdk
  }
}
