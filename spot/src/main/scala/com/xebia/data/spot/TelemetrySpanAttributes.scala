package com.xebia.data.spot

import io.opentelemetry.api.common.AttributeKey

import java.lang

/** Precomposed attribute keys. These aren't necessary in most cases (the `setAttribute` function has an overload that
  * takes a string), but I think it's useful to have them all in one overview.
  */
object TelemetrySpanAttributes {
  import AttributeKey._
  val appAttemptId: AttributeKey[String] = stringKey("spark.appAttemptId")
  val appId: AttributeKey[String] = stringKey("spark.appId")
  val appName: AttributeKey[String] = stringKey("spark.appName")

  val jobTime: AttributeKey[lang.Long] = longKey("spark.job.time")
  val jobStageId: AttributeKey[lang.Long] = longKey("spark.job.stage.id")
  val jobStageName: AttributeKey[String] = stringKey("spark.job.stage.name")
  val jobStageAttempt: AttributeKey[lang.Long] = longKey("spark.job.stage.attempt")
  val jobStageDiskSpill: AttributeKey[lang.Long] = longKey("spark.job.stage.diskBytesSpilled")
  val jobStageMemSpill: AttributeKey[lang.Long] = longKey("spark.job.stage.memoryBytesSpilled")
  val jobStagePeakExMem: AttributeKey[lang.Long] = longKey("spark.job.stage.peakExecutionMemory")
  val jobStageFailureReason: AttributeKey[String] = stringKey("spark.job.stage.failureReason")

  val sparkUser: AttributeKey[String] = stringKey("spark.user")
}
