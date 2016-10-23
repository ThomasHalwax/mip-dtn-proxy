package io.syncpoint.dtn;

import io.syncpoint.dtn.bundle.BundleReadAdapter;
import io.syncpoint.dtn.bundle.Flags;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MessageForwarder extends AbstractVerticle {
    private static final Logger LOGGER = LoggerFactory.getLogger(MessageForwarder.class);

    @Override
    public void start() {
        // we received a bundle from a remote source
        // messages are published by the DtnApiHandler
        vertx.eventBus().localConsumer(Addresses.EVENT_BUNDLE_RECEIVED, transport -> {

            BundleReadAdapter bundle = new BundleReadAdapter((JsonObject)transport.body());
            LOGGER.debug("received bundle from {} sent to {}", bundle.source(), bundle.destination());
            LOGGER.debug("destination is a singleton?: {}", bundle.getHeaderFlag(Flags.DESTINATION_IS_SINGLETON));

            if (Addresses.DTN_DCI_ANNOUNCE_ADDRESS.equals(bundle.destination())) {
                LOGGER.debug("forwarding dci announce to local broadcaster ...");
                bundle.blocks().forEachRemaining(b -> {
                    JsonObject block = (JsonObject)b;
                    vertx.eventBus().send(Addresses.COMMAND_ANNOUNCE_DCI, block.getString("payload"));
                });
            }
            else if (Addresses.DTN_DCI_REPLY_ADDRESS.equals(bundle.destination())) {
                LOGGER.debug("forwarding dci reply to wherever ...");
            }
            else {
                LOGGER.warn("unsupported url");
            }
        });

        // handle a locally received DCI
        vertx.eventBus().localConsumer(Addresses.EVENT_DCI_ANNOUNCED, transport -> {

        });
    }
}
