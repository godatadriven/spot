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
  override def spotConfig: Map[String, String] = Map(config: _*)
}

class NoopSdkProvider extends OpenTelemetrySdkProvider {
  override def get(config: Map[String, String]): OpenTelemetry = OpenTelemetry.noop()
}
