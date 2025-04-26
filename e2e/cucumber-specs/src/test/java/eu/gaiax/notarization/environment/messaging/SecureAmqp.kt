package eu.gaiax.notarization.environment.messaging;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.smallrye.common.annotation.Identifier;
import io.vertx.amqp.AmqpClientOptions
import io.vertx.core.net.PemKeyCertOptions;
import io.vertx.core.net.PemTrustOptions;
import jakarta.enterprise.inject.Produces
import jakarta.inject.Inject

class SecureAmqp {

    @Inject
    lateinit var config: AmqpConfig;

    @ConfigProperty(name = "amqp-username")
    lateinit var amqpUsername: String;
    @ConfigProperty(name = "amqp-password")
    lateinit var amqpPassword: String;
    @ConfigProperty(name = "amqp-host")
    lateinit var amqpHost: String;
    @ConfigProperty(name = "amqp-port")
    var amqpPort: Int = 0;

    @Produces
    @Identifier("secure-amqp-options")
    fun getNamedOptions(): AmqpClientOptions {

        val amqpOptions = AmqpClientOptions()
                .setUsername(amqpUsername)
                .setPassword(amqpPassword)
                .setHost(amqpHost)
                .setPort(amqpPort)
                .setSsl(config.ssl())
                .setHostnameVerificationAlgorithm("")
                .setConnectTimeout(30000)
                .setReconnectInterval(5000);

        if (config.caPath().isPresent) {
            val trust = PemTrustOptions()
                    .addCertPath(config.caPath().get());
            amqpOptions.setPemTrustOptions(trust);
        }
        if (config.tls().isPresent) {
            var tlsConfig = config.tls().get();
            val keycert = PemKeyCertOptions()
                    .addCertPath(tlsConfig.certPath())
                    .addKeyPath(tlsConfig.keyPath());
            amqpOptions.pemKeyCertOptions = keycert;
        }

        return amqpOptions;
    }
}
