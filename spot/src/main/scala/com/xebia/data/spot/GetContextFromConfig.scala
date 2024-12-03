package com.xebia.data.spot

import io.opentelemetry.context.propagation.TextMapGetter

import java.lang
import scala.collection.JavaConverters._

/** Bridges between Spark config and OpenTelemetry's context propagator system.
  */
class GetContextFromConfig extends TextMapGetter[Map[String, String]] {
  override def keys(carrier: Map[String, String]): lang.Iterable[String] = carrier.keys
    .filter(_.startsWith(SPOT_CONFIG_PREFIX))
    .map(_.substring(SPOT_CONFIG_PREFIX.length))
    .asJava

  override def get(carrier: Map[String, String], key: String): String = carrier.get(configPrefixed(key)).orNull
}
