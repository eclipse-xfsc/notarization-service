# Digital Signature Service (DSS)

For general information, please see the [DSS configuration section](../../../documentation/admin/dss.md).

## Installation of the Helm chart

After (optionally) customizing the `values.yaml` file, the DSS service can be
installed with the following command:

```bash
helm upgrade --install dss -f values.yaml oci://node-654e3bca7fbeeed18f81d7c7.ps-xaas.io/dev-ops/dss-demo-webapp
```
