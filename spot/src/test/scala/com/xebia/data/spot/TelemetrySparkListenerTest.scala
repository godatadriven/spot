package com.xebia.data.spot

import com.xebia.data.spot.TestingSdkProvider.testingSdk
import io.opentelemetry.api.OpenTelemetry
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
import org.scalatest.{BeforeAndAfterEach, Suite}
import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers

import java.time.Duration
import java.util

class TelemetrySparkListenerTest extends AnyFlatSpecLike with TelemetrySparkListenerTestSupport {
  import Matchers._

  behavior of "TelemetrySparkListener"

  it should "generate the expected Trace for simulated events" in {
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
}

trait TelemetrySparkListenerTestSupport extends Suite with BeforeAndAfterEach {
  lazy val tsl: TelemetrySparkListener = {
    val conf: SparkConf = new SparkConf()
    conf.set("com.xebia.data.spot.sdkProvider", classOf[TestingSdkProvider].getName)
    new TelemetrySparkListener(conf)
  }

  def advanceTimeBy(duration: Duration): Unit = TestingSdkProvider.clock.advance(duration)

  def getFinishedSpanItems: util.List[SpanData] = TestingSdkProvider.getFinishedSpanItems

  override protected def afterEach(): Unit = TestingSdkProvider.reset()
}

object TestingSdkProvider {
  private[spot] val clock: TestClock = TestClock.create()
  private[spot] val spanExporter: InMemorySpanExporter = InMemorySpanExporter.create()
  private[spot] val testingSdk: OpenTelemetrySdk = OpenTelemetrySdk.builder().setTracerProvider(
    SdkTracerProvider.builder().setClock(clock).setSampler(Sampler.alwaysOn()).addSpanProcessor(SimpleSpanProcessor.builder(spanExporter).build()).build()
  ).build()

  def getFinishedSpanItems: util.List[SpanData] = {
    spanExporter.flush()
    spanExporter.getFinishedSpanItems
  }

  def reset(): Unit = spanExporter.reset()
}

class TestingSdkProvider extends OpenTelemetrySdkProvider {
  override def get(config: Map[String, String]): OpenTelemetry = testingSdk
}

