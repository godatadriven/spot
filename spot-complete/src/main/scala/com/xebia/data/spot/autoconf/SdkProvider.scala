package com.xebia.data.spot.autoconf

import com.xebia.data.spot.OpenTelemetrySdkProvider
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk

class SdkProvider extends OpenTelemetrySdkProvider {

  override def get(config: Map[String, String]): OpenTelemetrySdk = {
    AutoConfiguredOpenTelemetrySdk.initialize().getOpenTelemetrySdk
  }
}
