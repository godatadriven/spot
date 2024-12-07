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
package com.xebia.data

/** Spot: Spark-OpenTelemetry integration.
  */
package object spot {

  /** Fully-qualified classname of the default SdkProvider implementation. This class is defined in the sibling project
    * and therefore unavailable on our compile-time classpath.
    */
  val DEFAULT_PROVIDER_FQCN = "com.xebia.data.spot.autoconf.SdkProvider"

  /** Common prefix for all our keys in the SparkConf. */
  val SPOT_CONFIG_PREFIX = "spark.com.xebia.data.spot."

  /** Returns the given config key as prefixed with [[SPOT_CONFIG_PREFIX]]. */
  @inline
  private[spot] final def configPrefixed(key: String): String = SPOT_CONFIG_PREFIX + key

  /** Returns the version of the Spot library. */
  def version(): String = classOf[TelemetrySparkListener].getPackage.getImplementationVersion
}
