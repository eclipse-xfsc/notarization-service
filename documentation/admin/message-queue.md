# Message queue

The `request-processing` service requires a message queue system that supports:
- AMQP 1.0
- CloudEvents. Client-side support is built into [Quarkus](https://quarkus.io/guides/amqp-reference#sending-cloud-events)

This document describes the configuration of a RabbitMQ server to meet the requirements.

For the client-side integration, see the developer documentation

## RabbitMQ Configuration

It is recommended to setup a TLS connection between the `request-processing` service and RabbitMQ.
This can be done via a [configuration file](https://www.rabbitmq.com/configure.html#config-file).
Some information about TLS are shown [here](https://www.rabbitmq.com/ssl.html).
In general, you need to generate a CA certificate and provide it to the RabbitMQ instance as well as the `request-processing` service.
With the CA certificate, you can issue two specific certificates, one for the RabbitMQ instance and one for the `request-processing` service.
Those certificates must be configured in the corresponding service.

A default configuration for RabbitMQ can look like this:

```properties
auth_backends.1 = internal

stomp.listeners.tcp.1 = 0.0.0.0:61613

auth_mechanisms.1 = PLAIN
auth_mechanisms.2 = AMQPLAIN
auth_mechanisms.3 = EXTERNAL

ssl_options.cacertfile = /notarization/ca_certificate.pem
ssl_options.certfile   = /notarization/server_certificate.pem
ssl_options.keyfile    = /notarization/server_key.pem

loopback_users.guest = false
```

Important ports that need to be exposed are:

* 5672 AMQP
* 15672 RabbitMQ management interface (optional)
* 15692 Prometheus
