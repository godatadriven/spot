package com.xebia.data.spot

import com.xebia.data.spot.TestingSdkProvider.testingSdk
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator
import io.opentelemetry.context.propagation.ContextPropagators
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter
import io.opentelemetry.sdk.testing.time.TestClock
import io.opentelemetry.sdk.trace.`export`.SimpleSpanProcessor
import io.opentelemetry.sdk.trace.data.{SpanData, StatusData}
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.samplers.Sampler
import org.apache.spark.SparkConf
import org.apache.spark.scheduler.{JobSucceeded, SparkListenerApplicationEnd, SparkListenerApplicationStart, SparkListenerJobEnd, SparkListenerJobStart}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers

import java.time.Duration
import java.util

class TelemetrySparkListenerTest extends AnyFlatSpecLike with BeforeAndAfterEach {
  override protected def afterEach(): Unit = TestingSdkProvider.reset()

  import Matchers._

  behavior of "TelemetrySparkListener"

  it should "generate the expected Trace for simulated events" in new TestTelemetrySparkListener() {
    tsl.onApplicationStart(SparkListenerApplicationStart("testapp", Some("ta123"), 100L, "User", Some("1"), None, None))
    advanceTimeBy(Duration.ofMillis(200))
    tsl.onJobStart(SparkListenerJobStart(1, 200L, Seq.empty, null))
    advanceTimeBy(Duration.ofMillis(5000))
    tsl.onJobEnd(SparkListenerJobEnd(1, 5000, JobSucceeded))
    advanceTimeBy(Duration.ofMillis(100))
    tsl.onApplicationEnd(SparkListenerApplicationEnd(5200L))

    val spans = getFinishedSpanItems
    spans should have length(2)

    // The spans are listed in order of having been ended.
    val appSpan = spans.get(1)
    val jobSpan = spans.get(0)

    assertThat(appSpan)
      .isSampled
      .hasEnded
      .hasNoParent
      .hasName("application-testapp")
      .hasAttribute(TelemetrySpanAttributes.appId, "ta123")
      .hasAttribute(TelemetrySpanAttributes.appName, "testapp")
      .hasAttribute(TelemetrySpanAttributes.appAttemptId, "1")
      .hasAttribute(TelemetrySpanAttributes.sparkUser, "User")

    assertThat(jobSpan)
      .isSampled
      .hasEnded
      .hasParent(appSpan)
      .hasName("job-00001")
      .hasStatus(StatusData.ok())
  }

  it should "get traceId from config if provided" in new TestTelemetrySparkListener("com.xebia.data.spot.traceparent" -> "00-0af7651916cd43dd8448eb211c80319c-b7ad6b7169203331-01") {
    tsl.onApplicationStart(SparkListenerApplicationStart("testapp", Some("ta123"), 100L, "User", Some("1"), None, None))
    tsl.onApplicationEnd(SparkListenerApplicationEnd(5200L))
    val appSpan = getFinishedSpanItems.get(0)
    assertThat(appSpan)
      .hasTraceId("0af7651916cd43dd8448eb211c80319c")
      .hasParentSpanId("b7ad6b7169203331")
      .isSampled
  }
}

class TestTelemetrySparkListener(extraConf: (String, String)*) {
  lazy val tsl: TelemetrySparkListener = {
    val conf: SparkConf = new SparkConf()
    conf.set("com.xebia.data.spot.sdkProvider", classOf[TestingSdkProvider].getName)
    conf.setAll(extraConf)
    new TelemetrySparkListener(conf)
  }

  def advanceTimeBy(duration: Duration): Unit = TestingSdkProvider.clock.advance(duration)

  def getFinishedSpanItems: util.List[SpanData] = TestingSdkProvider.getFinishedSpanItems
}

object TestingSdkProvider {
  private[spot] val clock: TestClock = TestClock.create()
  private[spot] val spanExporter: InMemorySpanExporter = InMemorySpanExporter.create()
  private[spot] val testingSdk: OpenTelemetrySdk = OpenTelemetrySdk.builder().setTracerProvider(
    SdkTracerProvider.builder().setClock(clock).setSampler(Sampler.alwaysOn()).addSpanProcessor(SimpleSpanProcessor.builder(spanExporter).build()).build()
  ).setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance())).build()

  def getFinishedSpanItems: util.List[SpanData] = {
    spanExporter.flush()
    spanExporter.getFinishedSpanItems
  }

  def reset(): Unit = spanExporter.reset()
}

class TestingSdkProvider extends OpenTelemetrySdkProvider {
  override def get(config: Map[String, String]): OpenTelemetry = testingSdk
}

