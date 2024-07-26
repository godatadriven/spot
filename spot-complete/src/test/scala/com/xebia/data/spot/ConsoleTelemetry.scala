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
