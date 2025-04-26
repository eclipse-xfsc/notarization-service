# Digital Signature Service

The digital signature service is used to validate document signatures.
A demonstration of the digital signature service is provided [here](https://github.com/esig/dss-demonstrations.git).

We're using a prepared image of the digital signature service: `node-654e3bca7fbeeed18f81d7c7.ps-xaas.io/dev-ops/dss@sha256:db37b0cb6e1e835155f24fca00f1ded7ddb0ffd4defa37e24cd29dca657779c5`.

See [here](https://gitlab.eclipse.org/eclipse/xfsc/dev-ops/components/dss) for more information.

## Configuration

There is no special configuration for the digital signature service needed.
You only have to set the `QUARKUS_REST_CLIENT_DSS_API_URL` environment variable for the `request-processing` service.
