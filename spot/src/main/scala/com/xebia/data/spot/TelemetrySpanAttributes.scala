package com.xebia.data.spot

import io.opentelemetry.api.common.AttributeKey

object TelemetrySpanAttributes {
  val appAttemptId: AttributeKey[String] = AttributeKey.stringKey("spark.appAttemptId")
  val appId: AttributeKey[String] = AttributeKey.stringKey("spark.appId")
  val appName: AttributeKey[String] = AttributeKey.stringKey("spark.appName")
  val sparkUser: AttributeKey[String] = AttributeKey.stringKey("spark.user")
}
