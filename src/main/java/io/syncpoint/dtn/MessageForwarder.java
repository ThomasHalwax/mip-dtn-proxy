package io.syncpoint.dtn;

import io.syncpoint.dtn.bundle.*;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.eventbus.DeliveryOptions;
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
                }
            } else {
                LOGGER.warn("no response for nodename query: {}", response.cause().getMessage());
                startup.fail(response.cause());
            }
        });

        // we received a bundle from a remote source
        // messages are published by the DtnApiHandler
        vertx.eventBus().localConsumer(Addresses.EVENT_BUNDLE_RECEIVED, transport -> {

            BundleAdapter bundle = new BundleAdapter((JsonObject)transport.body());
            LOGGER.debug("received bundle from {} sent to {}", bundle.getSource(), bundle.getDestination());

            URI sourceEndpoint;
            URI destinationEndpoint;
            try {
                sourceEndpoint = new URI(bundle.getSource());
                destinationEndpoint = new URI(bundle.getDestination());
                if (sourceEndpoint.getHost().equals(node.getHost())) {
                    LOGGER.debug("ignoring packets sent by self");
                    return;
                }
            } catch (URISyntaxException e) {
                LOGGER.warn("invalid URI, bundle will not be processed", e.getMessage());
                return;
            }

            addToResolver(bundle);

            String eventBusDestinationAddress;
            switch (bundle.getDestination()) {
                case Addresses.DTN_DCI_ANNOUNCE_ADDRESS: {
                    eventBusDestinationAddress = Addresses.COMMAND_ANNOUNCE_DCI;
                    break;
                }
                case Addresses.DTN_DCI_REPLY_ADDRESS: {
                    eventBusDestinationAddress = Addresses.COMMAND_REPLY_DCI;
                    break;
                }
                default: {
                    // a relative address which will be consumed on the eventbus
                    eventBusDestinationAddress = destinationEndpoint.getPath();
                    break;
                }
            }

            LOGGER.debug("forwarding base64 encoded message to local address {}", eventBusDestinationAddress);
            DeliveryOptions bundleReceivedOptions = new DeliveryOptions();
            bundleReceivedOptions.addHeader("source", sourceEndpoint.getPath());
            bundleReceivedOptions.addHeader("destination", destinationEndpoint.getPath());

            final String finalDestinationAddress = eventBusDestinationAddress;
            bundle.blockIterator().forEachRemaining(b -> {
                BlockAdapter block = new BlockAdapter((JsonObject)b);
                // TODO: only send payload block
                vertx.eventBus().publish(finalDestinationAddress, block.getEncodedContent(), bundleReceivedOptions);
            });
        });

        // handle a locally received DCI
        vertx.eventBus().localConsumer(Addresses.EVENT_DCI_ANNOUNCED, transport ->
                sendDci(Addresses.DTN_DCI_ANNOUNCE_ADDRESS, (String)transport.body()));

        // handle a locally received DCI
        vertx.eventBus().localConsumer(Addresses.EVENT_DCI_REPLIED, transport ->
                sendDci(Addresses.DTN_DCI_REPLY_ADDRESS, (String)transport.body()));

        vertx.eventBus().localConsumer(Addresses.EVENT_SOCKET_CLOSED, message -> {
            LOGGER.debug("handling {} from channel {}", Addresses.EVENT_SOCKET_CLOSED, message.body());

            final String remoteHost = resolver.getHostForChannel((String) message.body());
            final DtnUri destination = DtnUri.builder()
                    .host(remoteHost)
                    .application(Addresses.APP_PREFIX)
                    .process((String) message.body())
                    .build();

            BundleAdapter bundle = new BundleAdapter();
            bundle.setDestination(destination.toString());
            bundle.setSource((String)message.body());

            BundleFlagsAdapter flagsAdapter = new BundleFlagsAdapter();
            flagsAdapter.set(BundleFlags.DELIVERY_REPORT, true);
            bundle.setPrimaryBlockField(BundleFields.BUNDLE_FLAGS, String.valueOf(flagsAdapter.getFlags()));

            bundle.setPrimaryBlockField(BundleFields.REPORT_TO, Addresses.DTN_REPORT_TO_ADDRESS);

            BlockAdapter payload = new BlockAdapter();
            payload.setPlainContent("CLOSE_SOCKET");
            bundle.addBlock(payload.getBlock());

            vertx.eventBus().publish(Addresses.COMMAND_SEND_BUNDLE, bundle.getBundle());
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

            final DtnUri destination = DtnUri.builder()
                    .host(bundleHost)
                    .application(Addresses.APP_PREFIX)
                    .process(pdu.headers().get("destination"))
                    .build();

            final DtnUri source = DtnUri.builder()
                    .host(bundleHost)
                    .application(Addresses.APP_PREFIX)
                    .process(pdu.headers().get("source"))
                    .build();

            bundle.setDestination(destination.toString());
            bundle.setSource(source.toString());
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

        vertx.eventBus().localConsumer(Addresses.COMMAND_REGISTER_PROXY, localNodeAddress -> {
            String registrationAddress;
            String nodeAddress = (String)localNodeAddress.body();
            if (nodeAddress.startsWith(DtnUri.SCHEMA_AND_PREFIX)) {
                registrationAddress = nodeAddress;
            }
            else {
                DtnUri registrationUri = DtnUri.builder().
                        host(node.getHost()).
                        application(Addresses.APP_PREFIX).
                        process(localNodeAddress.body().toString()).
                        build();
                registrationAddress = registrationUri.toString();
            }

            vertx.eventBus().publish(Addresses.COMMAND_ADD_REGISTRATION, registrationAddress);
            LOGGER.debug("added registration for local DP proxy {}", registrationAddress);
        });

        vertx.eventBus().localConsumer(Addresses.COMMAND_UNREGISTER_PROXY, localNodeAddress -> {

            DtnUri registrationAddress = DtnUri.builder().
                    host(node.getHost()).
                    application(Addresses.APP_PREFIX).
                    process(localNodeAddress.body().toString()).
                    build();

            vertx.eventBus().publish(Addresses.COMMAND_DELETE_REGISTRATION, registrationAddress.toString());
            LOGGER.debug("removed registration for local DP proxy {}", registrationAddress.toString());
        });

        vertx.eventBus().localConsumer(Addresses.COMMAND_SEND_CLOSE_SOCKET, message -> {
            //TODO: forward message
            LOGGER.debug("handling and currently ignoring {}", Addresses.COMMAND_SEND_CLOSE_SOCKET);
        });

        vertx.eventBus().publish(Addresses.COMMAND_REGISTER_PROXY, Addresses.DTN_DCI_ANNOUNCE_ADDRESS);
        vertx.eventBus().publish(Addresses.COMMAND_REGISTER_PROXY, Addresses.DTN_DCI_REPLY_ADDRESS);
    }

    private void addToResolver(BundleAdapter bundle) {
        String channel = Helper.getChannelFromUri(bundle.getSource());
        if (resolver.hasHostForChannel(channel)) return;
        String remoteHost = Helper.getDtnHostFromUri(bundle.getSource());
        resolver.registerHostForChannel(channel, remoteHost);
        LOGGER.debug("registered host {} for channel {}", remoteHost, channel);
    }

    private void sendDci(String destination, String xmlDci) {
        BundleAdapter bundle = new BundleAdapter();
        bundle.setDestination(destination);

        DtnUri sourceUri = DtnUri.builder().
                host(node.getHost()).
                application(Addresses.APP_PREFIX).
                process(Helper.getElementValue("NodeID", xmlDci)).
                build();

        bundle.setSource(sourceUri.toString());

        BundleFlagsAdapter flags = new BundleFlagsAdapter();
        flags.set(BundleFlags.DESTINATION_IS_SINGLETON, false);
        flags.set(BundleFlags.DELETION_REPORT, true);
        bundle.setPrimaryBlockField(BundleFields.BUNDLE_FLAGS, String.valueOf(flags.getFlags()));

        DtnUri reportToUri = DtnUri.builder().
                host(node.getHost()).
                application(Addresses.DTN_REPORT_TO_ADDRESS).
                build();

        bundle.setPrimaryBlockField(BundleFields.REPORT_TO, reportToUri.toString());

        BlockAdapter payload = new BlockAdapter();
        payload.setPlainContent(xmlDci);
        bundle.addBlock(payload.getBlock());

        vertx.eventBus().publish(Addresses.COMMAND_SEND_BUNDLE, bundle.getBundle());
    }
}
