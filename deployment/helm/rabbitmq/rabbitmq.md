# RabbitMQ

For general information, please see the [message queue configuration
section](../../../documentation/admin/message-queue.md) and the [official website
of the
chart](https://github.com/bitnami/charts/tree/main/bitnami/rabbitmq).

## Installation of the Helm chart

After (optionally) customizing the `values.yaml` files, the RabbitMQ service can be
installed with the following command:

`helm install rabbitmq -f values.yaml oci://registry-1.docker.io/bitnamicharts/rabbitmq`

After the installation succeeded, the RabbitMQ UI is available under
<http://localhost:15672>, if the port 15672 is redirected:

`kubectl -n <the-namespace> port-forward svc/rabbitmq 15672:15672`
