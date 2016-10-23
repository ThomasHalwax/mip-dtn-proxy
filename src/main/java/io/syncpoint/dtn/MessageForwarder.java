package io.syncpoint.dtn;

import io.syncpoint.dtn.bundle.BundleAdapter;
import io.syncpoint.dtn.bundle.Flags;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MessageForwarder extends AbstractVerticle {
    private static final Logger LOGGER = LoggerFactory.getLogger(MessageForwarder.class);
    private static final String DCI_REPLY_ADDRESS = "/dem/dci/reply";
    private static final String DCI_ANNOUNCE_ADDRESS = "/dem/dci/announce";

    @Override
    public void start() {
        vertx.eventBus().localConsumer("bundle.received", transport -> {

            BundleAdapter bundle = new BundleAdapter((JsonObject)transport.body());
            LOGGER.debug("received bundle from {} sent to {}", bundle.source(), bundle.destination());
            LOGGER.debug("destination is a singleton?: {}", bundle.getHeaderFlag(Flags.DESTINATION_IS_SINGLETON));

            if (bundle.destination().endsWith(DCI_ANNOUNCE_ADDRESS)) {
                LOGGER.debug("forwarding dci announce to local broadcaster ...");
                bundle.blocks().forEachRemaining(b -> {
                    JsonObject block = (JsonObject)b;
                    vertx.eventBus().send("local://dem/dci/announce", block.getString("payload"));
                });
            }
            else if (bundle.destination().endsWith(DCI_REPLY_ADDRESS)) {
                LOGGER.debug("forwarding dci reply to wherever ...");
            }
            else {
                LOGGER.warn("unsupported url");
            }
        });
    }
}
