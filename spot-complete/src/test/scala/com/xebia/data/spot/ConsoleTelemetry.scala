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

import org.scalactic.source.Position
import org.scalatest.{BeforeAndAfter, Suite}

trait ConsoleTelemetry extends Suite with BeforeAndAfter {

  override protected def before(fun: => Any)(implicit pos: Position): Unit = {
    sys.props += "otel.logs.exporter" -> "console"
    sys.props += "otel.metrics.exporter" -> "console"
    sys.props += "otel.traces.exporter" -> "console"
  }

  override protected def after(fun: => Any)(implicit pos: Position): Unit = {
    sys.props -= "otel.logs.exporter"
    sys.props -= "otel.metrics.exporter"
    sys.props -= "otel.traces.exporter"
  }
}
