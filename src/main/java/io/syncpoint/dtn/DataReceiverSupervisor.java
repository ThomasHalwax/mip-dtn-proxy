package io.syncpoint.dtn;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public final class DataReceiverSupervisor extends AbstractVerticle {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataReceiverSupervisor.class);

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

            String tOpenRequestAddress = Addresses.PREFIX + Helper.findElementValue("NodeID", xmlDci);
            vertx.eventBus().localConsumer(tOpenRequestAddress, tOpenRequestHandler());
            vertx.eventBus().publish(Addresses.COMMAND_REGISTER_PROXY, tOpenRequestAddress);
        };
    }

    private void addToKnownDemInstances(String xmlDci) {
        final String nodeID = Helper.findElementValue("NodeID", xmlDci);
        final String replicationNodeIPAddress = Helper.findElementValue("ReplicationNodeIPAddress", xmlDci);
        final String replicationNodePort = Helper.findElementValue("ReplicationNodePort", xmlDci);

        JsonObject connectionInfo = new JsonObject();
        connectionInfo.put("ipAddress", replicationNodeIPAddress);
        connectionInfo.put("port", Integer.parseInt(replicationNodePort));

        knownDemInstances.put(nodeID, connectionInfo);
    }

    private Handler<Message<Object>> tOpenRequestHandler() {
        return message -> {
            LOGGER.debug("received T_OPEN_REQ, will deploy a new DataReceiverProxy for {}", message.body().toString());
        };

    }

}
