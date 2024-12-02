# Spot: Spark-OpenTelemetry
[![Build](https://github.com/godatadriven/spot/actions/workflows/ci.yml/badge.svg)](https://github.com/godatadriven/spot/actions/workflows/ci.yml)

This package connects [Apache Spark™][sp-home] to [OpenTelemetry][ot-home].

This allows reporting tracing and metrics from any Spark or PySpark job to [OpenTelemetry Collector][ot-col], or directly to any [supported backend][ot-export].

## Status

ℹ️This project is in initial development. It's not ready for use.

## Usage

The recommended way to use Spot relies on [OpenTelemetry Autoconfigure][ot-auto] to obtain the OpenTelemetry configuration. You pass the spot-complete jar to spark-submit to make Spot available to your job, and configure `spark.extraListeners` to enable it.

The jar filename embeds the two-digit Spark version, and the scala version:

```text
spot-complete-3.3_2.12-1.0.0.jar
              ↑↑↑ ↑↑↑↑ ↑↑↑↑↑
               A   B    C
A: Spark Version ∈ { 3.3, 3.4, 3.5 }
B: Scala Version ∈ { 2.12, 2.13 }
C: Spot library version
```

The spark and scala versions must match your spark process.

```diff
  SPARK_VERSION=3.5
  SCALA_VERSION=2.12
  spark-submit \
+     --jar com.xebia.data.spot.spot-complete-${SPARK_VERSION}_${SCALA_VERSION}-x.y.z.jar \
+     --conf spark.extraListeners=com.xebia.data.spot.TelemetrySparkListener \
      com.example.MySparkJob
```

### Context Propagation

To use context propagation, provide the necessary headers as SparkConf values. The default configuration uses [`traceparent`][traceparent]:

```diff
  SPARK_VERSION=3.5
  SCALA_VERSION=2.12
  spark-submit \
      --jar com.xebia.data.spot.spot-complete-${SPARK_VERSION}_${SCALA_VERSION}-x.y.z.jar \
      --conf spark.extraListeners=com.xebia.data.spot.TelemetrySparkListener \
+     --conf spark.com.xebia.data.spot.traceparent=00-1234abcd5678abcd-1234abcd-01 \
      com.example.MySparkJob
```

All SparkConf values that start with `spark.com.xebia.data.spot.` are made available to the `ContextPropagator`. If you use a different propagator than the default, you can prefix its required keys accordingly.

### Prerequisites

Instrumenting for telemetry is useless until you publish the recorded data somewhere. This might be the native metrics suite of your chosen cloud provider, or a free or commercial third party system such as Prometheus + Tempo + Grafana. You can have your instrumented Spark jobs publish directly to the backend, or run the traffic via OpenTelemetry Collector. Choosing the backend and routing architecture is outside the scope of this document.

If you're using Spark on top of Kubernetes, you should install and configure the [OpenTelemetry Operator][ot-k8s-oper]. In any other deployment you should publish the appropriate [environment variables for autoconf][ot-auto-env].

### Customizing OpenTelemetry AutoConfigure

The automatic configuration is controlled by a set of environment variables or JVM system properties. These are documented here: [configuration][otel-config].

#### As Environment Variables

Use any mechanism of choice, such as shell exports:

```bash
export OTEL_TRACES_EXPORTER=zipkin
export OTEL_EXPORTER_ZIPKIN_ENDPOINT=http://localhost:9411/api/v2/spans
```

Note: if you use the Kubernetes Operator, these environment variables are controlled there.

#### As JVM System Properties

Besides all the standard ways, JVM system properties can also be passed to Spot via the spark-submit command:

```diff
  SPARK_VERSION=3.5
  SCALA_VERSION=2.12
  spark-submit \
      --jar com.xebia.data.spot.spot-complete-${SPARK_VERSION}_${SCALA_VERSION}-x.y.z.jar \
      --conf spark.extraListeners=com.xebia.data.spot.TelemetrySparkListener \
+     --conf spark.otel.traces.exporter=zipkin \
+     --conf spark.exporter.zipkin.endpoint=http://localhost:9411/api/v2/spans \
      com.example.MySparkJob
```

All options starting with `spark.otel` are so exposed. Note: existing values are overwritten.

### Configuring OpenTelemetry SDK Manually

If the OpenTelemetry Autoconfigure mechanism doesn't meet your requirements, you can provide your own OpenTelemetry instance programmatically. This requires a few steps:

1. Write a class that implements `com.xebia.data.spot.OpenTelemetrySdkProvider`.
    ```scala
    package com.example
    import io.opentelemetry.api.OpenTelemetry
    import io.opentelemetry.sdk.OpenTelemetrySdk

    class MyCustomProvider extends OpenTelemetrySdkProvider {
      override def get(config: Map[String, String]): OpenTelemetry = OpenTelemetrySdk.builder()
        // customize SDK construction
        .build()
    }
    ```
2. Make the compiled class available to your Spark environment.
3. Add `spark.com.xebia.data.spot.sdkProvider` to your spark config, referencing your implementation.
    ```diff
      SPARK_VERSION=3.5
      SCALA_VERSION=2.12  # This will be 2.12 or 2.13, whichever matches your Spark deployment.
      spark-submit \
          --jar com.xebia.data.spot.spot-complete-${SPARK_VERSION}_${SCALA_VERSION}-x.y.z.jar \
          --conf spark.extraListeners=com.xebia.data.spot.TelemetrySparkListener \
    +     --conf spark.com.xebia.data.spot.sdkProvider=com.example.MyCustomProvider \
          com.example.MySparkJob
    ```

## Design Choices

### Why not simply use Spark's built-in DropWizard support?

Because that's something that already exists, and this is something I wanted to build. If the DropWizard metrics in Spark meet your needs, you should consider using those.

### Why not simply use Spark's JobHistory server?

In part for the same reason: because that's something that already exists, and this is something I wanted to build. There's obviously more of a difference here; the purpose of telemetry (whether that's DropWizard or OpenTelemetry) is to enable monitoring and alerting, whereas the JobHistory server is used reactively.

### Crash on initialization failure

If the OpenTelemetry SDK cannot be obtained during startup, we allow the listener –and enclosing spark job– to crash.

**Trade-off:** the enclosing spark job can run just fine without the telemetry listener. If we handle any initialization errors, we get out of the way of the instrumented business process.

**Rationale:** if you instrument the job, you expect to see your telemetry. Fail-fast behaviour ensures no telemetry is silently lost.

## Future Work

These are things that are out of scope for the moment:

1. Downstream propagation of trace context. It may be useful in some environments to forward the trace context to downstream systems such as data stores.
2. OpenTelemetry Airflow Plugin. If the Spark job is started by an Airflow DAG Run, it would be neat if some data from the DAG Run can be added to the OpenTelemetry context in Spot. Airflow could itself participate in distributed tracing: DAG Runs and Task Executions can be mapped as traces, with context propagation into the Spot Listener. In addition, key variables such as the data interval start and end could be made available as baggage.


[ot-auto]:     https://opentelemetry.io/docs/languages/java/instrumentation/#automatic-configuration
[ot-auto-env]: https://opentelemetry.io/docs/languages/java/configuration/
[ot-col]:      https://opentelemetry.io/docs/collector/
[otel-config]: https://opentelemetry.io/docs/languages/java/configuration/
[ot-export]:   https://opentelemetry.io/ecosystem/registry/?component=exporter
[ot-home]:     https://opentelemetry.io/
[ot-k8s-oper]: https://opentelemetry.io/docs/kubernetes/operator/
[sp-home]:     https://spark.apache.org
[traceparent]: https://www.w3.org/TR/trace-context/
