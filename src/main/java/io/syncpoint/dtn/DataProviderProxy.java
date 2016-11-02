package io.syncpoint.dtn;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.net.NetSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Base64;

public final class DataProviderProxy extends AbstractVerticle {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataProviderProxy.class);
    private static final Base64.Encoder encoder = Base64.getEncoder();
    private static final Base64.Decoder decoder = Base64.getDecoder();

    private static final int T_OPEN_REQ_LENGTH = 30;
    private static final int T_PREAMBLE_LENGTH = 10;
    private static final String ADDRESS_PREFIX = "dem://";
    private static final String TERMINAL_STRING = "00000002T9\r\n";
    private static final String T_CHKCON = "00000002T4";
    private static final String T_ABRT_PRECONDITION_FAILED = "00000006T3|412";
    private final NetSocket clientSocket;

    private String sourceNodeId;
    private String destinationNodeId;
    private Buffer buffer = Buffer.buffer(20);

    public DataProviderProxy(NetSocket clientSocket) {
        this.clientSocket = clientSocket;
    }

    @Override
    public void start() {

        clientSocket.handler(preT1Handler());

        clientSocket.closeHandler(socketClosed -> {
            vertx.eventBus().publish(Addresses.COMMAND_UNREGISTER_PROXY, ADDRESS_PREFIX + sourceNodeId);
            LOGGER.debug("socket was closed, undeploying handler verticle");
            vertx.undeploy(deploymentID(), undeploy -> {
                if (undeploy.succeeded()) {
                    LOGGER.info("un-deployment succeeded");
                }
                else {
                    LOGGER.warn("un-deployment failed", undeploy.cause());
                }
            });
        });
    }

    /**
     *
     * T_OPEN_REQ message (length = 30 characters)
     * 00000022T1|123456789|987654321
     *
     * @return a handler which waits for a T1 (T_OPEN_REQ) message
     */
    private Handler<Buffer> preT1Handler() {
        return data -> {

            LOGGER.debug(">> {}", data.toString());
            buffer.appendBuffer(data);
            if (buffer.length() >= T_OPEN_REQ_LENGTH) {
                final String lengthAndType = buffer.getString(0, T_PREAMBLE_LENGTH);

                // we expect a T1 open request message ...
                if (! ("00000022T1".equals(lengthAndType))) {
                    LOGGER.warn("unexpected length and type: {}", lengthAndType);
                    clientSocket.write(T_ABRT_PRECONDITION_FAILED);
                    clientSocket.end();
                }

                sourceNodeId = buffer.getString(11, 20);
                destinationNodeId = buffer.getString(21, T_OPEN_REQ_LENGTH);

                LOGGER.debug("received T_OPEN_REQ from {} to {}", sourceNodeId, destinationNodeId);

                String channelAddress = ADDRESS_PREFIX + destinationNodeId + "|" + sourceNodeId;
                vertx.eventBus().localConsumer(channelAddress, channelMessageHandler());
                vertx.eventBus().publish(Addresses.COMMAND_REGISTER_PROXY, channelAddress);

                DeliveryOptions tOpenRequestOptions = new DeliveryOptions();
                tOpenRequestOptions.addHeader("source", ADDRESS_PREFIX + sourceNodeId);
                tOpenRequestOptions.addHeader("destination", ADDRESS_PREFIX + destinationNodeId);
                vertx.eventBus().publish(Addresses.COMMAND_OPEN_TMAN_CONNECTION,
                        buffer.getString(0, T_OPEN_REQ_LENGTH),
                        tOpenRequestOptions
                );

                if (buffer.length() > T_OPEN_REQ_LENGTH) {
                    buffer = buffer.getBuffer(T_OPEN_REQ_LENGTH, buffer.length());
                }
                else {
                    buffer = Buffer.buffer();
                }
                clientSocket.handler(tManHandler());
            }
        };
    }

    /**
     *
     * @return a handler for all post T1 messages
     */
    private Handler<Buffer> tManHandler() {
        return data -> {
            if (TERMINAL_STRING.equals(data.toString())) {
                LOGGER.info("closing socket due to termination signal");
                clientSocket.end();
                return;
            }
            LOGGER.debug("sending {} from {} to {}", data, sourceNodeId, destinationNodeId);
        };
    }

    private Handler<Message<Object>> channelMessageHandler() {
        return message -> {
            LOGGER.debug("received data for local DEM");
        };
    }
}
