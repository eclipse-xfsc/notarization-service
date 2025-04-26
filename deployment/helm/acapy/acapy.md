# Aries Cloud Agent Python (ACA-Py)

For general information, please see the [ACA-Py configuration section](../../../documentation/admin/acapy.md).

## Installation of the Helm chart

After (optionally) customizing the `values.yaml` files, the ACA-Py services can be
installed with the following commands:

```sh
# Issuer
helm upgrade --install acapy -f values-acapy.yaml .
# Holder
helm upgrade --install acapy-holder -f values-acapy-holder.yaml .
```
