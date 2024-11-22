package com.xebia.data.spot

import io.opentelemetry.api.trace.{Span, StatusCode}
import io.opentelemetry.context.{Context, Scope}
import org.apache.spark.SparkConf
import org.apache.spark.scheduler.{JobSucceeded, SparkListener, SparkListenerApplicationEnd, SparkListenerApplicationStart, SparkListenerJobEnd, SparkListenerJobStart, SparkListenerStageCompleted}
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
 * @param sparkConf the `SparkConf`. This is provided automatically by the Spark application as it bootstraps.
 */
class TelemetrySparkListener(val sparkConf: SparkConf) extends SparkListener with OpenTelemetrySupport {
  import com.xebia.data.spot.TelemetrySparkListener.{PendingContext, PendingSpan}

  @transient
  protected val logger: Logger = LoggerFactory.getLogger(getClass.getName)
  logger.info(s"TelemetrySparkListener starting up: ${System.identityHashCode(this)}")

  override def spotConfig: Map[String, String] = sparkConf.getAll.toMap

  private var applicationSpan: Option[PendingSpan] = None
  private val jobSpans = mutable.Map[Int, PendingSpan]()

  lazy val rootContext: PendingContext = {
    logger.info(s"Find rootcontext; config is ${spotConfig}")
    val rc = openTelemetry.getPropagators.getTextMapPropagator.extract(Context.root(), spotConfig, new GetContextFromConfig())
    val scope = rc.makeCurrent()
    (rc, scope)
  }

  override def onApplicationStart(event: SparkListenerApplicationStart): Unit = {
    val sb = tracer.spanBuilder(s"application-${event.appName}")
      .setParent(rootContext._1)
      .setAttribute(TelemetrySpanAttributes.appName, event.appName)
      .setAttribute(TelemetrySpanAttributes.sparkUser, event.sparkUser)
    event.appId.foreach(sb.setAttribute(TelemetrySpanAttributes.appId, _))
    event.appAttemptId.foreach(sb.setAttribute(TelemetrySpanAttributes.appAttemptId, _))
    val span = sb.startSpan()
    val scope = span.makeCurrent()
    val context = span.storeInContext(rootContext._1)
    applicationSpan = Some((span, context, scope))
  }

  override def onApplicationEnd(event: SparkListenerApplicationEnd): Unit = {
    applicationSpan
      .map { case (span, _, scope) =>
        span.end()
        scope.close()
      }
      .orElse {
        logger.warn("Received onApplicationEnd, but found no tracing Span.")
        None
      }
    rootContext._2.close()
  }

    override def onJobStart(event: SparkListenerJobStart): Unit = {
      applicationSpan.foreach { case (_, parentContext, _) =>
        val span = tracer.spanBuilder("job-%05d".format(event.jobId))
          .setParent(parentContext)
          .startSpan()
        val scope = span.makeCurrent()
        val context = span.storeInContext(parentContext)
        jobSpans += event.jobId -> (span, context, scope)
      }
    }

    override def onJobEnd(event: SparkListenerJobEnd): Unit = {
      jobSpans.get(event.jobId).foreach { case (span, _, scope) =>
        event.jobResult match {
          case JobSucceeded => span.setStatus(StatusCode.OK)
          case _ => span.setStatus(StatusCode.ERROR)
        }
        span.end()
        scope.close()
      }
    }

  override def onStageCompleted(event: SparkListenerStageCompleted): Unit = {
    // event.stageInfo.rddInfos: memSize interessant
    // event.stageInfo.rddInfos.foreach(_.memSize)
    // event.stageInfo.taskMetrics.peakExecutionMemory
  }
}

object TelemetrySparkListener {
  type PendingContext = (Context, Scope)
  type PendingSpan = (Span, Context, Scope)
}
