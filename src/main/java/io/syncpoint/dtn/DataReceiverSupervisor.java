package io.syncpoint.dtn;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public final class DataReceiverSupervisor extends AbstractVerticle {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataReceiverSupervisor.class);
    private final Base64.Decoder decoder = Base64.getDecoder();

    // keys: DEM node IDs, values: a JSON object which holds IP address and port of the DEM
    private final Map<String, JsonObject> knownDemInstances = new HashMap<>();

    @Override
    public void start() {
        vertx.eventBus().localConsumer(Addresses.EVENT_DCI_ANNOUNCED, localDciHandler());
        vertx.eventBus().localConsumer(Addresses.EVENT_DCI_REPLIED, localDciHandler());
    }

    /**
     * The handler will populate a map {@code localDemInstances} with all data necessary to connect to a local DEM instance
     * @return a handler which consumes all DCI messages from local DEM instances.
     */
    private Handler<Message<Object>> localDciHandler() {
        return dci -> {
            LOGGER.debug("handling local DCI ");
            String xmlDci = dci.body().toString();
            addToKnownDemInstances(xmlDci);

            String tOpenRequestAddress = Helper.getElementValue("NodeID", xmlDci);
            vertx.eventBus().localConsumer("/" + Addresses.APP_PREFIX + "/" + tOpenRequestAddress, tOpenRequestHandler());
            vertx.eventBus().publish(Addresses.COMMAND_REGISTER_PROXY, tOpenRequestAddress);
        };
    }

    private void addToKnownDemInstances(String xmlDci) {
        final String nodeID = Helper.getElementValue("NodeID", xmlDci);
        final String replicationNodeIPAddress = Helper.getElementValue("ReplicationNodeIPAddress", xmlDci);
        final String replicationNodePort = Helper.getElementValue("ReplicationNodePort", xmlDci);

        // TODO: would be better to use the resolver and the ip://address:port URI syntax
        JsonObject connectionInfo = new JsonObject();
        connectionInfo.put("ipAddress", replicationNodeIPAddress);
        connectionInfo.put("port", Integer.parseInt(replicationNodePort));

        knownDemInstances.put(nodeID, connectionInfo);
    }

    private Handler<Message<Object>> tOpenRequestHandler() {
        return message -> {
            String tOpenRequest = new String(decoder.decode((String)message.body()));
            LOGGER.debug("received T_OPEN_REQ {}", tOpenRequest);
            String destinationNodeId = Helper.getDestinationNodeId(tOpenRequest);

            if (! knownDemInstances.containsKey(destinationNodeId)) {
                LOGGER.warn("received T_OPEN_REQ for an unknown DEM instance: {}", destinationNodeId);
                return;
            }

            URI peerUri;
            try {
                peerUri = new URI(message.headers().get("source"));
            } catch (URISyntaxException e) {
                LOGGER.warn("source is not a valid URI: {}", e.getMessage());
                return;
            }

            JsonObject config = new JsonObject();
            config.put("connectionInfo", knownDemInstances.get(destinationNodeId));
            config.put("peerId", peerUri.getPath());
            config.put("tOpenRequest", tOpenRequest);

            DeploymentOptions drOptions = new DeploymentOptions().setConfig(config);
            vertx.deployVerticle(DataReceiverProxy.class.getName(), drOptions, deployment -> {
                if (deployment.failed()) {
                    LOGGER.warn("failed to deploy new verticle for T_OPEN_REQ: {}", deployment.cause().getMessage());
                }
                else {
                    LOGGER.debug("deployed verticle for peerId {}", peerUri.getPath());
                }
            });
        };
    }
}
