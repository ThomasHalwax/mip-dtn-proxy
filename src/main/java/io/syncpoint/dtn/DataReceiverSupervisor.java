package io.syncpoint.dtn;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class DataReceiverSupervisor extends AbstractVerticle {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataReceiverSupervisor.class);

    @Override
    public void start() {
        vertx.eventBus().localConsumer(Addresses.EVENT_DCI_ANNOUNCED, localDciHandler());
        vertx.eventBus().localConsumer(Addresses.EVENT_DCI_REPLYED, localDciHandler());
    }

    private Handler<Message<Object>> localDciHandler() {
        return dci -> {
            LOGGER.debug("handling local DCI");
        };
    }
}
