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
import java.util.function.Consumer;

public final class DataProviderProxy extends AbstractVerticle {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataProviderProxy.class);

    private final NetSocket clientSocket;
    private final Base64.Decoder decoder = Base64.getDecoder();
    private final TmanPduParser parser = new TmanPduParser();

    private String sourceNodeId;
    private String destinationNodeId;
    private String localEndpointAddress;
    private String remoteEndpointAddress;

    public DataProviderProxy(NetSocket clientSocket) {
        this.clientSocket = clientSocket;
    }

    @Override
    public void start() {

        parser.errorHandler( error -> {
            LOGGER.error("parsing caused an error: {}", error.getMessage());
            clientSocket.end();
        });
        parser.handler(preT1Handler());

        clientSocket.handler(parser::addData);
        clientSocket.closeHandler(socketClosed -> {
            LOGGER.debug("socket was closed, undeploying handler verticle");
            if (hasValidAddresses()) {
                vertx.eventBus().publish(Addresses.COMMAND_UNREGISTER_PROXY, localEndpointAddress);
                vertx.eventBus().publish(Addresses.COMMAND_SEND_CLOSE_SOCKET, remoteEndpointAddress);
            }
            vertx.undeploy(deploymentID());
        });

        LOGGER.debug("proxy for socket {} is ready, waiting for T_OPEN_REQ", clientSocket.localAddress().host());
        clientSocket.resume();
    }

    /**
       *
     * @return a handler which waits for a T1 (T_OPEN_REQ) message
     */
    private Consumer<TManPdu> preT1Handler() {
        return tManPdu -> {
            LOGGER.debug(">> {}", tManPdu.getPdu().toString());

            if (! TManPduType.T_OPNRQ.equals(tManPdu.getPduType())) {
                LOGGER.warn("received PDU of type {} while waiting to T_OPEN_REQUEST", tManPdu.getPduType());
                clientSocket.close();
                return;
            }

            sourceNodeId =  Helper.getSourceNodeId(tManPdu.getPdu().toString());
            destinationNodeId = Helper.getDestinationNodeId(tManPdu.getPdu().toString());

            LOGGER.debug("received T_OPEN_REQ from {} to {}", sourceNodeId, destinationNodeId);

            // remote data will be sent to this address
            // read: output from destination will be piped to input at source
            localEndpointAddress = destinationNodeId + "/" + sourceNodeId;

            // this is the source address fo all subsequent TMAN PDUs
            // read: output from source will be piped to destination
            remoteEndpointAddress = sourceNodeId + "/" + destinationNodeId;
            vertx.eventBus().localConsumer(localEndpointAddress, remoteToSocketHandler());
            vertx.eventBus().publish(Addresses.COMMAND_REGISTER_PROXY, localEndpointAddress);

            DeliveryOptions tOpenRequestOptions = new DeliveryOptions();
            tOpenRequestOptions.addHeader("source", sourceNodeId);
            tOpenRequestOptions.addHeader("destination", destinationNodeId);
            vertx.eventBus().publish(Addresses.COMMAND_SEND_TMAN_PDU,
                    tManPdu.getPdu().toString(),
                    tOpenRequestOptions
            );

            parser.handler(socketToRemoteHandler());
        };
    }

    /**
     *
     * @return a handler for all post T1 messages
     */

    private Consumer<TManPdu> socketToRemoteHandler() {
        return pdu -> {
            LOGGER.debug("sending {} from {} to {}", pdu.getPduType(), sourceNodeId, destinationNodeId);
            DeliveryOptions pduOptions = new DeliveryOptions();
            pduOptions.addHeader("source", sourceNodeId);
            pduOptions.addHeader("destination", remoteEndpointAddress);
            vertx.eventBus().publish(Addresses.COMMAND_SEND_TMAN_PDU, pdu.getPdu(), pduOptions);
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

    private boolean hasValidAddresses() {
        return ((localEndpointAddress != null) && (remoteEndpointAddress != null));
    }
}