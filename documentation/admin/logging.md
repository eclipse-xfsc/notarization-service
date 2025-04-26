# Logging

For the Java services (`oidc-identity-resolver`, `profile`, `request-processing`, `revocation` and `scheduler`) logging can be configured by using the environment variable `QUARKUS_LOG_LEVEL`.
The default log level is set to `WARN`.
More information about logging in the Javas services can be found [here](https://quarkus.io/guides/logging#logging-levels).
For the `ssi-issuance` service, the environment variable `LOG_LEVEL` can be used, and the default level is `warn`.

By default, the applications write their logs to standard out (stdout).
This is sufficient for many deployment environments, such as [Kubernetes](https://kubernetes.io/docs/concepts/cluster-administration/logging/).
Some approaches on how to configure logging with a logging framework on Kubernetes can also be found at the provided URL.

By default, logging frameworks, such as Graylog, fluentd or logstash are not directly supported out of the box.
The Quarkus applications must be reconfigured and rebuilt to enable support for these centralized logging solutions, see the [guide here](https://quarkus.io/guides/centralized-log-management) on how to reconfigure the applications.
For the Node.js application, a reconfiguration and rebuild might also be necessary, see the logging [guide here](https://github.com/iamolegga/nestjs-pino) on how to configure the Node.js app.
The ACA-Py which is used for credential issuance provides its own documentation about logging, see [here](https://github.com/hyperledger/aries-cloudagent-python/blob/main/Logging.md).
