package io.syncpoint.dtn;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageConsumer;
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
    private String peerId;
    private MessageConsumer<Object> eventbusConsumer;

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
            // only perform unregister is there was an T_OPEN_REQ before
            if (peerId != null) {
                vertx.eventBus().publish(Addresses.COMMAND_UNREGISTER_PROXY, peerId);
                vertx.eventBus().publish(Addresses.COMMAND_SEND_CLOSE_SOCKET, "/" + Addresses.APP_PREFIX + "/"+ peerId);
            }
            if (eventbusConsumer != null) {
                eventbusConsumer.unregister();
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
            // peerId is a unique URI for identifying the communication between two peers
            peerId = destinationNodeId + "/" + sourceNodeId + "/" + deploymentID();

            eventbusConsumer = vertx.eventBus().localConsumer("/" + Addresses.APP_PREFIX + "/" + peerId, remoteToSocketHandler());
            LOGGER.debug("consuming messages on address {}", eventbusConsumer.address());

            vertx.eventBus().publish(Addresses.COMMAND_REGISTER_PROXY, peerId);

            DeliveryOptions tOpenRequestOptions = new DeliveryOptions();
            tOpenRequestOptions.addHeader("source", peerId);
            // destination must be the "common" address of the remote DEM
            tOpenRequestOptions.addHeader("destination", "/" + Addresses.APP_PREFIX + "/" + destinationNodeId);
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
            LOGGER.debug("sending {} to peer {}", pdu.getPduType(), peerId);
            DeliveryOptions pduOptions = new DeliveryOptions();
            String peerAddress = "/" + Addresses.APP_PREFIX + "/" + peerId;
            pduOptions.addHeader("source", peerAddress);
            pduOptions.addHeader("destination", peerAddress);
            vertx.eventBus().publish(Addresses.COMMAND_SEND_TMAN_PDU, pdu.getPdu(), pduOptions);
        };
    }

    private Handler<Message<Object>> remoteToSocketHandler() {
        return encodedMessage -> {
            LOGGER.debug("received data for local DEM");
            final byte[] message = decoder.decode((String) encodedMessage.body());
            if ("CLOSE_SOCKET".equals(new String(message))) {
                LOGGER.info("closing local socket due to a command from a remote proxy");
                // TODO: replace close socket handler to avoid sending a colose to the peer
                clientSocket.end();
                return;
            }
            clientSocket.write(Buffer.buffer(message));
        };
    }
}