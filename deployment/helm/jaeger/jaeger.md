# Jaeger

For general information, please see the [monitoring configuration
section](../../../documentation/admin/monitoring.md) and the [official website
of the
chart](https://github.com/jaegertracing/helm-charts/tree/main/charts/jaeger).

## Installation of the Helm chart

After (optionally) customizing the `values.yaml` files, the Jaeger service can be
installed with the following commands:

```bash
helm repo add jaegertracing https://jaegertracing.github.io/helm-charts
helm install jaeger -f values.yaml jaegertracing/jaeger
```

After the installation succeeded, the Jaeger Query UI is available under
<http://localhost:16686>, if the port 16686 is redirected:

`kubectl -n <the-namespace> port-forward svc/jaeger-query 16686:16686`
