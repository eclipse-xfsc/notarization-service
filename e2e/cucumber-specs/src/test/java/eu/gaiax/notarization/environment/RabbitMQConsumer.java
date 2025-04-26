/*
 */
package eu.gaiax.notarization.environment;

import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import io.vertx.core.json.JsonObject;
import java.util.Optional;
import java.util.function.Supplier;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 *
 * @author Mike Prechtl
 */
@ApplicationScoped
public class RabbitMQConsumer {

    public static final Logger LOG = LoggerFactory.getLogger(RabbitMQConsumer.class);

    public static final String INCOMING_OPERATOR_REQUEST_CHANGED = "operator-request-changed";
    public static final String INCOMING_REQUESTOR_REQUEST_CHANGED = "requestor-request-changed";

    public static final int DEFAULT_TIMEOUT_SECS = 5;

    private JsonObject latestOperatorMsg;
    private JsonObject latestRequestorMsg;

	@Before
	public void before(Scenario scenario) {
		latestOperatorMsg = null;
        latestRequestorMsg = null;
	}

    @Incoming(INCOMING_OPERATOR_REQUEST_CHANGED)
    public void consumeOperatorMsg(JsonObject msg) {
       latestOperatorMsg = msg;
    }

    @Incoming(INCOMING_REQUESTOR_REQUEST_CHANGED)
    public void consumeRequestorMsg(JsonObject msg) {
       latestRequestorMsg = msg;
    }

    public boolean checkForOperatorMessage(String msgType) {
        return checkMessage(() -> latestOperatorMsg, msgType, DEFAULT_TIMEOUT_SECS);
    }

    public boolean checkForOperatorMessage(String msgType, int timeoutInSeconds) {
        return checkMessage(() -> latestOperatorMsg, msgType, timeoutInSeconds);
    }

    public boolean checkForRequestorMessage(String msgType) {
        return checkMessage(() -> latestRequestorMsg, msgType, DEFAULT_TIMEOUT_SECS);
    }

    public boolean checkForRequestorMessage(String msgType, int timeoutInSeconds) {
        return checkMessage(() -> latestRequestorMsg, msgType, timeoutInSeconds);
    }

    public boolean checkMessage(Supplier<JsonObject> msg, String msgType, int timeoutInSeconds) {
        var optionalMsg = Optional.ofNullable(msg.get());

        while (optionalMsg.isEmpty() && timeoutInSeconds-- > 0) {
            sleepOneSecond();
            optionalMsg = Optional.ofNullable(msg.get());
        }

        return optionalMsg
                .map(m -> {
                    LOG.info("Received following message: {}", m.encodePrettily());
                    return m.getString("msg").equals(msgType);
                })
                .orElse(false);
    }

    private void sleepOneSecond() {
        try {
            Thread.sleep(1000);
            LOG.info("Waiting one second for incoming message...");
        } catch (InterruptedException ex) { }
    }

}
