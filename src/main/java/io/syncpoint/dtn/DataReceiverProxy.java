package io.syncpoint.dtn;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.NetClient;
import io.vertx.core.net.NetSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class DataReceiverProxy extends AbstractVerticle {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataReceiverProxy.class);
    private NetSocket socket;
    private String channelAddress;

    @Override
    public void start() {
        JsonObject connectionInfo = config().getJsonObject("connectionInfo");
        String tOpenRequest = config().getString("tOpenRequest");

        NetClient client = vertx.createNetClient();
        client.connect(connectionInfo.getInteger("port"), connectionInfo.getString("ipAddress"), attempt -> {
            if (attempt.succeeded()) {
                socket = attempt.result();
                LOGGER.debug("connected to DEM instance");

                channelAddress = Addresses.PREFIX + Helper.getSourceNodeId(tOpenRequest) + "|" + Helper.getDestinationNodeId(tOpenRequest);
                vertx.eventBus().localConsumer(channelAddress, remoteToSocketHandler());
                vertx.eventBus().publish(Addresses.COMMAND_REGISTER_PROXY, channelAddress);

                socket.handler(socketToRemoteHandler());
                socket.closeHandler(socketClosedHandler());
                socket.write(tOpenRequest);
            }
            else {
                LOGGER.warn("failed to connect: {}", attempt.cause().getMessage());
                // TODO: notify sender of message that connection failed!
            }
        });
    }

    private Handler<Message<Object>> remoteToSocketHandler() {
        return data -> {
            LOGGER.debug("will send {} to socket", data.toString());
        };
    }

    private Handler<Buffer> socketToRemoteHandler() {
        return data -> {
            LOGGER.debug("will send {} to remote", data.toString());
            // TODO: forward data to receiving DTN side
        };
    }

    private Handler<Void> socketClosedHandler() {
        return undef -> {
            LOGGER.warn("socket closed");
            vertx.eventBus().publish(Addresses.COMMAND_UNREGISTER_PROXY, channelAddress);
            vertx.undeploy(deploymentID());
        };
    }
}
