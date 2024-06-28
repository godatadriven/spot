# Spot: Spark-OpenTelemetry
[![Build](https://github.com/godatadriven/spot/actions/workflows/ci.yml/badge.svg)](https://github.com/godatadriven/spot/actions/workflows/ci.yml)

This package connects [Apache Spark™][sp-home] to [OpenTelemetry][ot-home].

This creates a layer of indirection to allow reporting metrics from any Spark or PySpark job to [OpenTelemetry Collector][ot-col], or directly to any [supported backend][ot-export].

## Status

ℹ️This project is in initial development. It's not ready for use.

## Usage

The recommended way to use Spot relies on [OpenTelemetry Autoconfigure][ot-auto] to obtain the OpenTelemetry configuration. You pass the `spot-complete-*.jar` to spark-submit to make Spot available to your job, and configure `spark.extraListeners` to enable it.

```bash
SCALA_VERSION=2.12  # This will be 2.12 or 2.13, whichever matches your Spark deployment.
spark-submit \
    --jar com.xebia.data.spot.spot-complete_${SCALA_VERSION}-x.y.z.jar \
    --conf spark.extraListeners=com.xebia.data.spot.TelemetrySparkListener \
    com.example.MySparkJob
```

### Prerequisites

Instrumenting for telemetry is useless until you publish the recorded data somewhere. This might be the native metrics suite of your chosen cloud provider, or a free or commercial third party system such as Prometheus + Tempo + Grafana. You can have your instrumented Spark jobs publish directly to the backend, or run the traffic via OpenTelemetry Collector. Choosing the backend and routing architecture is outside the scope of this document.

If you're using Spark on top of Kubernetes, you should install and configure the [OpenTelemetry Operator][ot-k8s-oper]. In any other deployment you should publish the appropriate [environment variables for autoconf][ot-auto-env].

[ot-auto]:     https://opentelemetry.io/docs/languages/java/instrumentation/#automatic-configuration
[ot-auto-env]: https://opentelemetry.io/docs/languages/java/configuration/
[ot-col]:      https://opentelemetry.io/docs/collector/
[ot-export]:   https://opentelemetry.io/ecosystem/registry/?component=exporter
[ot-home]:     https://opentelemetry.io/
[ot-k8s-oper]: https://opentelemetry.io/docs/kubernetes/operator/
[sp-home]:     https://spark.apache.org
