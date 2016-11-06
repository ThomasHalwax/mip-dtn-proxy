package io.syncpoint.dtn;

import io.syncpoint.dtn.bundle.*;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MessageForwarder extends AbstractVerticle {
    private static final Logger LOGGER = LoggerFactory.getLogger(MessageForwarder.class);
    private String nodename = "";

    @Override
    public void start() {
        vertx.eventBus().send(Addresses.QUERY_NODENAME, "", response -> {
            if (response.succeeded()) {
                LOGGER.debug("got response to nodename query: {}", response.result().body());
                nodename = response.result().body().toString();
                if (! nodename.endsWith("/")) {
                    nodename = nodename + "/";
                }
            } else {
                LOGGER.warn("no response for nodename query: {}", response.cause().getMessage());
            }
        });

        // we received a bundle from a remote source
        // messages are published by the DtnApiHandler
        vertx.eventBus().localConsumer(Addresses.EVENT_BUNDLE_RECEIVED, transport -> {

            BundleAdapter bundle = new BundleAdapter((JsonObject)transport.body());
            LOGGER.debug("received bundle from {} sent to {}", bundle.getSource(), bundle.getDestination());

            String destinationAddress;
            if (Addresses.DTN_DCI_ANNOUNCE_ADDRESS.equals(bundle.getDestination())) {
                destinationAddress = Addresses.COMMAND_ANNOUNCE_DCI;
            }
            else if (Addresses.DTN_DCI_REPLY_ADDRESS.equals(bundle.getDestination())) {
                destinationAddress = Addresses.COMMAND_REPLY_DCI;
            }
            else {
                destinationAddress = bundle.getDestination().replace(
                        Addresses.DTN_PREFIX + Addresses.APP_PREFIX,"");
            }

            LOGGER.debug("forwarding base64 encoded message to local address {}", destinationAddress);
            bundle.blockIterator().forEachRemaining(b -> {
                BlockAdapter block = new BlockAdapter((JsonObject)b);
                // TODO: only send payload block
                vertx.eventBus().publish(destinationAddress, block.getEncodedContent());
            });
        });

        // handle a locally received DCI
        vertx.eventBus().localConsumer(Addresses.EVENT_DCI_ANNOUNCED, transport -> {
            sendDci(Addresses.DTN_DCI_ANNOUNCE_ADDRESS, (String)transport.body());
        });

        // handle a locally received DCI
        vertx.eventBus().localConsumer(Addresses.EVENT_DCI_REPLIED, transport -> {
            sendDci(Addresses.DTN_DCI_REPLY_ADDRESS, (String)transport.body());
        });

        /**
         * handles the messages created by a DataProviderProxy instance. A T_OPEN_REQ
         * was issued by a DEM instance and this request contains the source and destination
         * nodeID. We use this data to send a bundle to the appropriate DTN destination.
         * The {@link DataReceiverSupervisor} listens to all bundles which destinations are
         * the known (local) DEM nodeIDs
         *
         * After the "channel" is established between a {@link DataProviderProxy} and a {@link DataReceiverProxy}
         * the instances will communicate without the interception of the {@link MessageForwarder}.
         *
         */
        vertx.eventBus().localConsumer(Addresses.COMMAND_SEND_TMAN_PDU, pdu -> {
            String tOpenRequest = pdu.body().toString();

            BundleAdapter bundle = new BundleAdapter();
            bundle.setDestination(Addresses.APP_PREFIX + pdu.headers().get("destination"));
            bundle.setSource(nodename + Addresses.APP_PREFIX + pdu.headers().get("source"));
            BundleFlagsAdapter flags = new BundleFlagsAdapter();

            flags.set(BundleFlags.DELIVERY_REPORT, true);
            // TODO: verify the correct semantics of the flag
            //flags.set(BundleFlags.DESTINATION_IS_SINGLETON, true);
            bundle.setPrimaryBlockField(BundleFields.BUNDLE_FLAGS, String.valueOf(flags.getFlags()));

            bundle.setPrimaryBlockField(BundleFields.REPORT_TO, Addresses.DTN_REPORT_TO_ADDRESS);

            BlockAdapter payload = new BlockAdapter();
            payload.setPlainContent(tOpenRequest);
            bundle.addBlock(payload.getBlock());

            vertx.eventBus().publish(Addresses.COMMAND_SEND_BUNDLE, bundle.getBundle());
            LOGGER.debug("consumed {} and forwarded bundle", Addresses.COMMAND_SEND_TMAN_PDU);
        });

        vertx.eventBus().localConsumer(Addresses.EVENT_SOCKET_CLOSED, message -> {
            LOGGER.debug("handling {}", Addresses.EVENT_SOCKET_CLOSED);

            BundleAdapter bundle = new BundleAdapter();
            bundle.setDestination(Addresses.DTN_PREFIX + Addresses.APP_PREFIX + message.body());

            BundleFlagsAdapter flagsAdapter = new BundleFlagsAdapter();
            flagsAdapter.set(BundleFlags.DELIVERY_REPORT, true);
            bundle.setPrimaryBlockField(BundleFields.BUNDLE_FLAGS, String.valueOf(flagsAdapter.getFlags()));

            bundle.setPrimaryBlockField(BundleFields.REPORT_TO, Addresses.DTN_REPORT_TO_ADDRESS);

            BlockAdapter payload = new BlockAdapter();
            payload.setPlainContent("CLOSE_SOCKET");
            bundle.addBlock(payload.getBlock());

            vertx.eventBus().publish(Addresses.COMMAND_SEND_BUNDLE, bundle.getBundle());
        });

        vertx.eventBus().localConsumer(Addresses.COMMAND_REGISTER_PROXY, localNodeAddress -> {
            String localDPProxyAddress = localNodeAddress.body().toString();
            vertx.eventBus().publish(Addresses.COMMAND_ADD_REGISTRATION, localDPProxyAddress);
            LOGGER.debug("added registration for local DP proxy {}", localDPProxyAddress);
        });

        vertx.eventBus().localConsumer(Addresses.COMMAND_UNREGISTER_PROXY, localNodeAddress -> {
            String localDPProxyAddress = localNodeAddress.body().toString();
            vertx.eventBus().publish(Addresses.COMMAND_DELETE_REGISTRATION, localDPProxyAddress);
            LOGGER.debug("removed registration for local DP proxy {}", localDPProxyAddress);
        });


        vertx.eventBus().localConsumer(Addresses.COMMAND_SEND_CLOSE_SOCKET, message -> {
            //TODO: forward message
            LOGGER.debug("handling {}", Addresses.COMMAND_SEND_CLOSE_SOCKET);
        });
    }

    private void sendDci(String destination, String xmlDci) {
        BundleAdapter bundle = new BundleAdapter();
        bundle.setDestination(destination);
        bundle.setSource(nodename + Addresses.APP_PREFIX + Helper.getElementValue("NodeID", xmlDci));

        BundleFlagsAdapter flags = new BundleFlagsAdapter();
        flags.set(BundleFlags.DESTINATION_IS_SINGLETON, false);
        flags.set(BundleFlags.DELETION_REPORT, true);
        bundle.setPrimaryBlockField(BundleFields.BUNDLE_FLAGS, String.valueOf(flags.getFlags()));

        bundle.setPrimaryBlockField(BundleFields.REPORT_TO, nodename + Addresses.DTN_REPORT_TO_ADDRESS);

        BlockAdapter payload = new BlockAdapter();
        payload.setPlainContent(xmlDci);
        bundle.addBlock(payload.getBlock());

        vertx.eventBus().publish(Addresses.COMMAND_SEND_BUNDLE, bundle.getBundle());
    }
}
