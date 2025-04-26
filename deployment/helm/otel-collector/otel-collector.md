# OpenTelemetry

For general information, please see the [monitoring configuration
section](../../../documentation/admin/monitoring.md) and the [official website
of the chart](https://opentelemetry.io/docs/kubernetes/helm/).

## Installation of the Helm chart

After (optionally) customizing the `values.yaml` files, the OpenTelemetry service can be
installed with the following commands:

```bash
helm repo add open-telemetry https://open-telemetry.github.io/opentelemetry-helm-charts
helm install otel-collector -f values.yaml open-telemetry/opentelemetry-collector
```
