package com.xebia.data

/**
 * Spot: Spark-OpenTelemetry integration.
 */
package object spot {
  def version(): String = classOf[TelemetrySparkListener].getPackage.getImplementationVersion
}
