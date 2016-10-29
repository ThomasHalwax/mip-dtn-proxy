package io.syncpoint.dtn;

import io.syncpoint.dtn.bundle.BundleCreateAdapter;
import io.syncpoint.dtn.bundle.BundleFlags;
import io.syncpoint.dtn.bundle.BundleReadAdapter;
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
            JsonObject dci = new JsonObject((String)transport.body());
            BundleCreateAdapter bundle = new BundleCreateAdapter();
            bundle.setDestination(Addresses.DTN_DCI_ANNOUNCE_ADDRESS);
            bundle.setSource("dtn://" + dci.getJsonObject("DCI").getJsonObject("DciBody").getInteger("NodeID"));
            bundle.setHeaderFlags(BundleFlags.DESTINATION_IS_SINGLETON, false);


            bundle.addPayload(dci.encode());

            vertx.eventBus().publish(Addresses.COMMAND_SEND_BUNDLE, bundle.getCopy());
            LOGGER.debug("consumed {} and forwarded bundle to {}", Addresses.EVENT_DCI_ANNOUNCED, Addresses.COMMAND_SEND_BUNDLE);

        });
    }
}
