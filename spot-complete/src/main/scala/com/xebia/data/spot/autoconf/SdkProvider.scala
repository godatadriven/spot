package com.xebia.data.spot.autoconf

import com.xebia.data.spot.OpenTelemetrySdkProvider
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk
import org.slf4j.LoggerFactory

import java.util.concurrent.TimeUnit


/**
 * Uses OpenTelemetry Autoconfigure to build an OpenTelemetry SDK.
 *
 * To configure the autoconf SDK, see [[https://opentelemetry.io/docs/languages/java/configuration/]]. If you're on
 * Kubernetes, have a look at the OpenTelemetry Operator.
 */
class SdkProvider extends OpenTelemetrySdkProvider {
  private val logger = LoggerFactory.getLogger(classOf[SdkProvider])

  override def get(config: Map[String, String]): OpenTelemetrySdk = {
    logger.info("Using AutoConfigured OpenTelemetry SDK.")
    val sdk = AutoConfiguredOpenTelemetrySdk.initialize().getOpenTelemetrySdk

    sys.addShutdownHook {
      logger.info("Allowing up to 1 minute for OpenTelemetry SDK to shut down.")
      val completion = sdk.shutdown()
      completion.whenComplete(() => {
        completion.getFailureThrowable match {
          case e: Throwable => logger.warn(s"OpenTelemetry SDK shut down with Exception: ${e.toString}", e)
          case _ => logger.info("OpenTelemetry SDK shut down successfully.")
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
