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
