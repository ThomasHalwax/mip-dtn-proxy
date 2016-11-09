package io.syncpoint.dtn;

import io.syncpoint.dtn.bundle.*;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;

public final class MessageForwarder extends AbstractVerticle {
    private static final Logger LOGGER = LoggerFactory.getLogger(MessageForwarder.class);
    private final Resolver resolver = new Resolver();
    private URI node;

    @Override
    public void start(Future<Void> startup) {
        vertx.eventBus().send(Addresses.QUERY_NODENAME, "", response -> {
            if (response.succeeded()) {
                LOGGER.debug("got response to nodename query: {}", response.result().body());
                try {
                    node = new URI((String)response.result().body());
                } catch (URISyntaxException e) {
                    LOGGER.warn("invalid nodename", e.getMessage());
                    startup.fail(e);
                    return;
                }
            } else {
                LOGGER.warn("no response for nodename query: {}", response.cause().getMessage());
                startup.fail(response.cause());
                return;
            }
        });

        // we received a bundle from a remote source
        // messages are published by the DtnApiHandler
        vertx.eventBus().localConsumer(Addresses.EVENT_BUNDLE_RECEIVED, transport -> {

            BundleAdapter bundle = new BundleAdapter((JsonObject)transport.body());
            LOGGER.debug("received bundle from {} sent to {}", bundle.getSource(), bundle.getDestination());

            URI sendingEndpoint;
            URI localEndpoint;
            try {
                sendingEndpoint = new URI(bundle.getSource());
                localEndpoint = new URI(bundle.getDestination());
                if (sendingEndpoint.getHost().equals(node.getHost())) {
                    LOGGER.debug("ignoring packets sent by self");
                    return;
                }
            } catch (URISyntaxException e) {
                LOGGER.warn("invalid URI, bundle will not be processed", e.getMessage());
                return;
            }

            register(bundle);

            String destinationAddress;
            switch (bundle.getDestination()) {
                case Addresses.DTN_DCI_ANNOUNCE_ADDRESS: {
                    destinationAddress = Addresses.COMMAND_ANNOUNCE_DCI;
                    break;
                }
                case Addresses.DTN_DCI_REPLY_ADDRESS: {
                    destinationAddress = Addresses.COMMAND_REPLY_DCI;
                    break;
                }
                default: {
                    // possible uris:
                    // dtn://dem/nodeID (for T_OPEN_REQUESTS)
                    // dtn://myhost.name/sender/localReceiver
                    destinationAddress = localEndpoint.getPath();

                    break;
                }
            }

            LOGGER.debug("forwarding base64 encoded message to local address {}", destinationAddress);
            final String finalDestinationAddress = destinationAddress;
            bundle.blockIterator().forEachRemaining(b -> {
                BlockAdapter block = new BlockAdapter((JsonObject)b);
                // TODO: only send payload block
                vertx.eventBus().publish(finalDestinationAddress, block.getEncodedContent());
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
            String bundleHost = resolver.getHostForChannel(pdu.headers().get("destination"));
            if (bundleHost == null) {
                bundleHost = Addresses.DTN_PREFIX.substring(0, Addresses.DTN_PREFIX.length() - 2);
            }

            bundle.setDestination(bundleHost + "/" + Addresses.APP_PREFIX + pdu.headers().get("destination"));
            bundle.setSource(node + "/" + Addresses.APP_PREFIX + pdu.headers().get("source"));
            BundleFlagsAdapter flags = new BundleFlagsAdapter();

            flags.set(BundleFlags.DELIVERY_REPORT, true);

            if (resolver.hasHostForChannel(pdu.headers().get("destination"))) {
                flags.set(BundleFlags.DESTINATION_IS_SINGLETON, true);
            }
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
            localDPProxyAddress = node + "/" + Addresses.APP_PREFIX + localDPProxyAddress;
            vertx.eventBus().publish(Addresses.COMMAND_ADD_REGISTRATION, localDPProxyAddress);
            LOGGER.debug("added registration for local DP proxy {}", localDPProxyAddress);
        });

        vertx.eventBus().localConsumer(Addresses.COMMAND_UNREGISTER_PROXY, localNodeAddress -> {
            String localDPProxyAddress = localNodeAddress.body().toString();
            localDPProxyAddress = Addresses.APP_PREFIX + localDPProxyAddress;
            vertx.eventBus().publish(Addresses.COMMAND_DELETE_REGISTRATION, localDPProxyAddress);
            LOGGER.debug("removed registration for local DP proxy {}", localDPProxyAddress);
        });

        vertx.eventBus().localConsumer(Addresses.COMMAND_SEND_CLOSE_SOCKET, message -> {
            //TODO: forward message
            LOGGER.debug("handling {}", Addresses.COMMAND_SEND_CLOSE_SOCKET);
        });
    }

    private void register(BundleAdapter bundle) {
        String channel = Helper.getChannelFromUri(bundle.getSource(), Addresses.APP_PREFIX);
        if (resolver.hasHostForChannel(channel)) return;
        String remoteHost = Helper.getDtnHostFromUri(bundle.getSource());
        resolver.registerHostForChannel(channel, remoteHost);
        LOGGER.debug("registered host {} for channel {}", remoteHost, channel);
    }

    private void sendDci(String destination, String xmlDci) {
        BundleAdapter bundle = new BundleAdapter();
        bundle.setDestination(destination);
        bundle.setSource(node + "/" + Addresses.APP_PREFIX + Helper.getElementValue("NodeID", xmlDci));

        BundleFlagsAdapter flags = new BundleFlagsAdapter();
        flags.set(BundleFlags.DESTINATION_IS_SINGLETON, false);
        flags.set(BundleFlags.DELETION_REPORT, true);
        bundle.setPrimaryBlockField(BundleFields.BUNDLE_FLAGS, String.valueOf(flags.getFlags()));

        bundle.setPrimaryBlockField(BundleFields.REPORT_TO, node + "/" + Addresses.DTN_REPORT_TO_ADDRESS);

        BlockAdapter payload = new BlockAdapter();
        payload.setPlainContent(xmlDci);
        bundle.addBlock(payload.getBlock());

        vertx.eventBus().publish(Addresses.COMMAND_SEND_BUNDLE, bundle.getBundle());
    }
}
