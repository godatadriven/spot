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

import com.xebia.data.spot.TelemetrySparkListener.ApplicationSpan
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.trace.{Span, StatusCode}
import io.opentelemetry.context.{Context, Scope}
import org.apache.spark.SparkConf
import org.apache.spark.scheduler.{
  JobSucceeded,
  SparkListener,
  SparkListenerApplicationEnd,
  SparkListenerApplicationStart,
  SparkListenerJobEnd,
  SparkListenerJobStart,
  SparkListenerStageCompleted
}
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.mutable

/** A SparkListener that publishes job telemetry to OpenTelemetry.
  *
  * Usage:
  * {{{
  *   spark-submit \
  *       --conf=spark.extraListeners=com.xebia.data.spot.TelemetrySparkListener \
  *       com.example.MySparkJob
  * }}}
  *
  * @param sparkConf
  *   the `SparkConf`. This is provided automatically by the Spark application as it bootstraps.
  */
class TelemetrySparkListener(val sparkConf: SparkConf) extends SparkListener with OpenTelemetrySupport {
  import com.xebia.data.spot.TelemetrySparkListener.{PendingContext, PendingSpan}
  import com.xebia.data.spot.{TelemetrySpanAttributes => atts}

  @transient
  protected val logger: Logger = LoggerFactory.getLogger(getClass.getName)
  logger.info(s"TelemetrySparkListener starting up: ${System.identityHashCode(this)}")

  override def spotConfig: Map[String, String] = sparkConf.getAll.toMap

  private var applicationSpan: Option[ApplicationSpan] = None
  private val jobSpans = mutable.Map[Int, PendingSpan]()
  private val stageIdToJobId = mutable.Map[Int, Int]()

  lazy val rootContext: PendingContext = {
    logger.info(s"Find rootcontext; config is ${spotConfig}")
    val rc =
      openTelemetry.getPropagators.getTextMapPropagator.extract(Context.root(), spotConfig, new GetContextFromConfig())
    val scope = rc.makeCurrent()
    (rc, scope)
  }

  override def onApplicationStart(event: SparkListenerApplicationStart): Unit = {
    val sb = tracer
      .spanBuilder(s"application-${event.appName}")
      .setParent(rootContext._1)
      .setAttribute(atts.appName, event.appName)
      .setAttribute(atts.sparkUser, event.sparkUser)
    event.appId.foreach(sb.setAttribute(atts.appId, _))
    event.appAttemptId.foreach(sb.setAttribute(atts.appAttemptId, _))
    val span = sb.startSpan()
    val scope = span.makeCurrent()
    val context = span.storeInContext(rootContext._1)
    applicationSpan = Some(ApplicationSpan(span, context, scope))
  }

  override def onApplicationEnd(event: SparkListenerApplicationEnd): Unit = {
    applicationSpan
      .map { case ApplicationSpan(span, _, scope, status) =>
        status match {
          case StatusCode.UNSET => span.setStatus(StatusCode.OK)
          case _                => span.setStatus(status)
        }
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
    applicationSpan.foreach { case ApplicationSpan(_, parentContext, _, _) =>
      val span = tracer
        .spanBuilder("job-%05d".format(event.jobId))
        .setParent(parentContext)
        .startSpan()
      stageIdToJobId ++= event.stageIds.map(stageId => stageId -> event.jobId)
      span.setAttribute("stageIds", event.stageIds.mkString(","))
      val scope = span.makeCurrent()
      val context = span.storeInContext(parentContext)
      jobSpans += event.jobId -> (span, context, scope)
    }
  }

  override def onJobEnd(event: SparkListenerJobEnd): Unit = {
    jobSpans.get(event.jobId).foreach { case (span, _, scope) =>
      event.jobResult match {
        case JobSucceeded   => span.setStatus(StatusCode.OK)
        case jobFailed: Any =>
          // The JobFailed(e) case class is private[spark], therefore we can't use span.recordException(e).
          // TODO test with a Spark Job that succeeds after a task retry, what does that look like?
          applicationSpan = applicationSpan.map(_.failed())
          span.setStatus(StatusCode.ERROR, jobFailed.toString)
      }
      span.setAttribute(atts.jobTime, Long.box(event.time))
      span.end()
      scope.close()
    }
  }

  override def onStageCompleted(event: SparkListenerStageCompleted): Unit = {
    logger.info(s"onStageCompleted: $event, parentIds=${event.stageInfo.parentIds}")
    stageIdToJobId.get(event.stageInfo.stageId).map(jobSpans).foreach { case (span, _, _) =>
      val atb = Attributes.builder()
      atb.put(atts.jobStageId, event.stageInfo.stageId)
      atb.put(atts.jobStageName, event.stageInfo.name)
      atb.put(atts.jobStageAttempt, event.stageInfo.attemptNumber())
      atb.put(atts.jobStageDiskSpill, Long.box(event.stageInfo.taskMetrics.diskBytesSpilled))
      atb.put(atts.jobStageMemSpill, Long.box(event.stageInfo.taskMetrics.memoryBytesSpilled))
      atb.put(atts.jobStagePeakExMem, Long.box(event.stageInfo.taskMetrics.peakExecutionMemory))
      event.stageInfo.failureReason.foreach { reason =>
        atb.put(atts.jobStageFailureReason, reason)
      }
      span.addEvent("stageCompleted", atb.build())
    }
    stageIdToJobId -= event.stageInfo.stageId
  }
}

object TelemetrySparkListener {
  type PendingContext = (Context, Scope)
  type PendingSpan = (Span, Context, Scope)
  case class ApplicationSpan(span: Span, context: Context, scope: Scope, status: StatusCode = StatusCode.UNSET) {
    def failed(): ApplicationSpan = this.copy(status = StatusCode.ERROR)
  }
}
