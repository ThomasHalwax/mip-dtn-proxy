package io.syncpoint.dtn;

import io.vertx.core.AbstractVerticle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class DtnPayloadDecoder extends AbstractVerticle {
    private static final Logger LOGGER = LoggerFactory.getLogger(DtnPayloadDecoder.class);

    @Override
    public void start() {
        vertx.eventBus().localConsumer("bundle.received", message -> {
            LOGGER.debug("received a message of type {}", message.getClass().getName());
        });
    }
}
