package io.syncpoint.dtn;

import io.syncpoint.dtn.bundle.*;
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

            BundleAdapter bundle = new BundleAdapter((JsonObject)transport.body());
            LOGGER.debug("received bundle from {} sent to {}", bundle.getSource(), bundle.getDestination());

            if (Addresses.DTN_DCI_ANNOUNCE_ADDRESS.equals(bundle.getDestination())) {
                LOGGER.debug("forwarding dci announce to local broadcaster ...");
                bundle.blockIterator().forEachRemaining(b -> {
                    BlockAdapter block = new BlockAdapter((JsonObject)b);
                    vertx.eventBus().send(Addresses.COMMAND_ANNOUNCE_DCI, block.getEncodedContent());
                });
            }
            else if (Addresses.DTN_DCI_REPLY_ADDRESS.equals(bundle.getDestination())) {
                LOGGER.debug("forwarding dci reply to local broadcaster ...");
                bundle.blockIterator().forEachRemaining(b -> {
                    BlockAdapter block = new BlockAdapter((JsonObject)b);
                    vertx.eventBus().send(Addresses.COMMAND_REPLY_DCI, block.getEncodedContent());
                });
            }
            else {
                // must be
            }
        });

        // handle a locally received DCI
        vertx.eventBus().localConsumer(Addresses.EVENT_DCI_ANNOUNCED, transport -> {

            String xmlDci = (String)transport.body();

            BundleAdapter bundle = new BundleAdapter();
            bundle.setDestination(Addresses.DTN_DCI_ANNOUNCE_ADDRESS);
            bundle.setSource(Addresses.DTN_DCI_REPLY_ADDRESS);
            BundleFlagsAdapter flags = new BundleFlagsAdapter();
            flags.set(BundleFlags.DESTINATION_IS_SINGLETON, false);

            bundle.setPrimaryBlockField(BundleFields.HEADER, String.valueOf(flags.getFlags()));
            BlockAdapter payload = new BlockAdapter();
            payload.setPlainContent(xmlDci);
            bundle.addBlock(payload.getBlock());

            vertx.eventBus().publish(Addresses.COMMAND_SEND_BUNDLE, bundle.getBundle());
            LOGGER.debug("consumed {} and forwarded bundle to {}", Addresses.EVENT_DCI_ANNOUNCED, Addresses.COMMAND_SEND_BUNDLE);

        });
    }
}
