package com.xebia.data.spot

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should

class AutoconfiguredOpenTelemetrySdkProviderTest extends AnyFlatSpec with should.Matchers with ConsoleTelemetry {

  behavior of "OpenTelemetrySupport"

  it should "use the AutoConfiguredOpenTelemetrySdk if no config is provided" in {
    val uh = new TestOpenTelemetrySupport()
    // TODO improve verification;
    uh.openTelemetry should not be (null)
    uh.openTelemetry.toString should matchPattern {
      case s: String if s.contains("attributes={service.name=\"this is a test\"") =>
    }
  }
}

private[this] class TestOpenTelemetrySupport extends OpenTelemetrySupport {
  override def spotConfig: Map[String, String] = Map("spark.otel.service.name" -> "this is a test")
}
