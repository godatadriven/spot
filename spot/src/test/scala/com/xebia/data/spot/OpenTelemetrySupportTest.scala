package com.xebia.data.spot

import io.opentelemetry.api.OpenTelemetry
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should

class OpenTelemetrySupportTest extends AnyFlatSpec with should.Matchers {

  behavior of "OpenTelemetrySupport"

  it should "reflectively create an SDK provider based on 'spot.sdkProvider' config" in {
    val uh = new NoopOpenTelemetrySupport("spark.com.xebia.data.spot.sdkProvider" -> classOf[NoopSdkProvider].getName)
    uh.openTelemetry should be theSameInstanceAs OpenTelemetry.noop()
  }
}

class NoopOpenTelemetrySupport(config: (String, String)*) extends OpenTelemetrySupport {
  override def spotConfig: Map[String, String] = Map(config:_*)
}

class NoopSdkProvider extends OpenTelemetrySdkProvider {
  override def get(config: Map[String, String]): OpenTelemetry = OpenTelemetry.noop()
}
