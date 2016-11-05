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

    private static final int T_OPEN_REQ_LENGTH = 30;
    private static final int T_PREAMBLE_LENGTH = 10;
    private static final String TERMINAL_STRING = "00000002T9\r\n";
    private static final String T_ABRT_PRECONDITION_FAILED = "00000006T3|412";
    private final NetSocket clientSocket;
    private final Base64.Decoder decoder = Base64.getDecoder();

    private String sourceNodeId;
    private String destinationNodeId;
    private String localEndpointAddress;
    private String remoteEndpointAddress;

    private Buffer buffer = Buffer.buffer(20);

    public DataProviderProxy(NetSocket clientSocket) {
        this.clientSocket = clientSocket;
    }

    @Override
    public void start() {

        clientSocket.handler(preT1Handler());

        clientSocket.closeHandler(socketClosed -> {
            vertx.eventBus().publish(Addresses.COMMAND_UNREGISTER_PROXY, Addresses.PREFIX + localEndpointAddress);
            if (remoteEndpointAddress != null) {
                vertx.eventBus().publish(Addresses.COMMAND_SEND_CLOSE_SOCKET, remoteEndpointAddress);
            }
            LOGGER.debug("socket was closed, undeploying handler verticle");
            vertx.undeploy(deploymentID());
        });

        LOGGER.debug("proxy for socket {} is ready", clientSocket.localAddress().host());
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

                // remote data will be sent to this address
                // read: output from destination will be piped to input at source
                localEndpointAddress = destinationNodeId + "|" + sourceNodeId;

                // this is the source address fo all subsequent TMAN PDUs
                // read: output from source will be piped to destination
                remoteEndpointAddress = sourceNodeId + "|" + destinationNodeId;
                vertx.eventBus().localConsumer(localEndpointAddress, remoteToSocketHandler());
                // TODO: register proxy should be handeled by the message forwarder
                vertx.eventBus().publish(Addresses.COMMAND_REGISTER_PROXY, Addresses.PREFIX + localEndpointAddress);

                DeliveryOptions tOpenRequestOptions = new DeliveryOptions();
                tOpenRequestOptions.addHeader("source", sourceNodeId);
                tOpenRequestOptions.addHeader("destination", destinationNodeId);
                vertx.eventBus().publish(Addresses.COMMAND_SEND_TMAN_PDU,
                        buffer.getString(0, T_OPEN_REQ_LENGTH),
                        tOpenRequestOptions
                );

                if (buffer.length() > T_OPEN_REQ_LENGTH) {
                    buffer = buffer.getBuffer(T_OPEN_REQ_LENGTH, buffer.length());
                }
                else {
                    buffer = Buffer.buffer();
                }
                clientSocket.handler(socketToRemoteHandler());
            }
        };
    }

    /**
     *
     * @return a handler for all post T1 messages
     */
    private Handler<Buffer> socketToRemoteHandler() {

        TmanPduParser parser = new TmanPduParser();

        parser.errorHandler( error -> {
            LOGGER.error("parsing caused an error: {}", error.getMessage());
            clientSocket.close();
        });

        parser.handler(pdu -> {
            LOGGER.debug("sending {} from {} to {}", pdu.getPduType(), sourceNodeId, destinationNodeId);
            DeliveryOptions pduOptions = new DeliveryOptions();
            pduOptions.addHeader("source", sourceNodeId);
            pduOptions.addHeader("destination", remoteEndpointAddress);
            vertx.eventBus().publish(Addresses.COMMAND_SEND_TMAN_PDU, pdu.getPdu(), pduOptions);
        });


        return data -> {
            parser.addData(data);
        };
    }

    private Handler<Message<Object>> remoteToSocketHandler() {
        return encodedMessage -> {
            LOGGER.debug("received data for local DEM");
            final byte[] message = decoder.decode((String) encodedMessage.body());
            if ("CLOSE_SOCKET".equals(new String(message))) {
                LOGGER.info("closing local socket due to a command from a remote proxy");
                clientSocket.end();
                return;
            }
            clientSocket.write(Buffer.buffer(message));
        };
    }
}
