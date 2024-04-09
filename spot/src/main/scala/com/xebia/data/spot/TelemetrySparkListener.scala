package com.xebia.data.spot

import io.opentelemetry.api.trace.{Span, StatusCode}
import io.opentelemetry.context.Context
import org.apache.spark.SparkConf
import org.apache.spark.scheduler.{JobFailed, JobSucceeded, SparkListener, SparkListenerApplicationEnd, SparkListenerApplicationStart, SparkListenerJobEnd, SparkListenerJobStart, SparkListenerStageCompleted}
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.mutable

/**
 * A SparkListener that publishes job telemetry to OpenTelemetry.
 *
 * Usage:
 * {{{
 *   spark-submit \
 *       --conf=spark.extraListeners=com.xebia.data.spot.TelemetrySparkListener \
 *       com.example.MySparkJob
 * }}}
 *
 * @param conf the `SparkConf`. This is provided automatically by the Spark application as it bootstraps.
 */
class TelemetrySparkListener(val sparkConf: SparkConf) extends SparkListener with OpenTelemetrySupport {
  @transient
  protected lazy val logger: Logger = LoggerFactory.getLogger(getClass.getName)

  override val conf = OpenTelemetryConfig.from(sparkConf)
  private var applicationSpan: Option[(Span, Context)] = None
  private val jobSpans = mutable.Map[Int, (Span, Context)]()

  override def onApplicationStart(event: SparkListenerApplicationStart): Unit = {
    val sb = tracer.spanBuilder(s"application-${event.appName}")
      .setAttribute(TelemetrySpanAttributes.appName, event.appName)
      .setAttribute(TelemetrySpanAttributes.sparkUser, event.sparkUser)
    event.appId.foreach(sb.setAttribute(TelemetrySpanAttributes.appId, _))
    event.appId.foreach(sb.setAttribute(TelemetrySpanAttributes.appAttemptId, _))
    val span = sb.startSpan()
    val context = span.storeInContext(Context.root())
    applicationSpan = Some((span, context))
  }

  override def onApplicationEnd(event: SparkListenerApplicationEnd): Unit = {
    applicationSpan
      .map { case (span, _) =>
        span.end()
      }
      .orElse {
        logger.warn("Received onApplicationEnd, but found no tracing Span.")
        None
      }
  }

    override def onJobStart(event: SparkListenerJobStart): Unit = {
      applicationSpan.foreach { case (_, parentContext) =>
        val span = tracer.spanBuilder("job-%05d".format(event.jobId))
          .setParent(parentContext)
          .startSpan()
        val context = span.storeInContext(parentContext)
        jobSpans += event.jobId -> (span, context)
      }
    }

    override def onJobEnd(event: SparkListenerJobEnd): Unit = {
      jobSpans.get(event.jobId).foreach { case (span, _) =>
        event.jobResult match {
          case JobSucceeded => span.setStatus(StatusCode.OK)
          case _ => span.setStatus(StatusCode.ERROR)
        }
        span.end()
      }
    }

  override def onStageCompleted(event: SparkListenerStageCompleted): Unit = {
    // event.stageInfo.rddInfos: memSize interessant
    // event.stageInfo.rddInfos.foreach(_.memSize)
    // event.stageInfo.taskMetrics.peakExecutionMemory
  }
}
