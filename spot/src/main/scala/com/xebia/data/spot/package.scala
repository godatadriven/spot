package com.xebia.data

/**
 * Spot: Spark-OpenTelemetry integration.
 */
package object spot {
  /** Fully-qualified classname of the default SdkProvider implementation. This class is defined in the sibling project
   * and therefore unavailable on our compile-time classpath. */
  val DEFAULT_PROVIDER_FQCN = "com.xebia.data.spot.autoconf.SdkProvider"

  /** Common prefix for all our keys in the SparkConf. */
  val SPOT_CONFIG_PREFIX = "spark.com.xebia.data.spot."

  /** Returns the given config key as prefixed with [[SPOT_CONFIG_PREFIX]]. */
  @inline
  private[spot] final def configPrefixed(key: String): String = SPOT_CONFIG_PREFIX + key

  /** Returns the version of the Spot library. */
  def version(): String = classOf[TelemetrySparkListener].getPackage.getImplementationVersion
}
