package io.syncpoint.dtn;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.NetClient;
import io.vertx.core.net.NetClientOptions;
import io.vertx.core.net.NetSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Base64;

public final class DataReceiverProxy extends AbstractVerticle {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataReceiverProxy.class);
    private NetSocket socket;
    private Base64.Decoder decoder = Base64.getDecoder();
    private String peerId;

    @Override
    public void start(Future<Void> startup) {
        JsonObject connectionInfo = config().getJsonObject("connectionInfo");
        String tOpenRequest = config().getString("tOpenRequest");
        peerId = config().getString("peerId");

        NetClientOptions clientOptions = new NetClientOptions();
        clientOptions.setTcpKeepAlive(true);

        NetClient client = vertx.createNetClient(clientOptions);
        client.connect(connectionInfo.getInteger("port"), connectionInfo.getString("ipAddress"), attempt -> {
            if (attempt.succeeded()) {
                socket = attempt.result();
                LOGGER.debug("connected to DEM instance");
                vertx.eventBus().localConsumer(peerId, remoteToSocketHandler());
                vertx.eventBus().publish(Addresses.COMMAND_REGISTER_PROXY, peerId);

                socket.handler(socketToRemoteHandler());
                socket.closeHandler(socketClosedHandler());
                socket.write(tOpenRequest);
                startup.complete();
            }
            else {
                LOGGER.warn("failed to connect: {}", attempt.cause().getMessage());
                vertx.eventBus().publish(Addresses.EVENT_SOCKET_CLOSED, peerId);
                startup.fail(attempt.cause());
            }
        });
    }

    private Handler<Message<Object>> remoteToSocketHandler() {
        return encodedPdu -> {
            LOGGER.debug("sending PDU to socket");
            final byte[] pdu = decoder.decode((String) encodedPdu.body());
            socket.write(Buffer.buffer(pdu));
        };
    }

    private Handler<Buffer> socketToRemoteHandler() {
        TmanPduParser parser = new TmanPduParser();

        parser.handler(tManPdu -> {
            LOGGER.debug("will send {} to remote", tManPdu.getPduType().toString());
            DeliveryOptions sendPduOptions = new DeliveryOptions();
            sendPduOptions.addHeader("source", peerId);
            sendPduOptions.addHeader("destination", peerId);
            vertx.eventBus().publish(Addresses.COMMAND_SEND_TMAN_PDU, tManPdu.getPdu(), sendPduOptions);
        });

        parser.errorHandler(error -> {
            LOGGER.warn("received invalid data", error);
            socket.end();
        });

        return parser::addData;
    }

    private Handler<Void> socketClosedHandler() {
        return undef -> {
            LOGGER.warn("socket closed");
            vertx.eventBus().publish(Addresses.COMMAND_UNREGISTER_PROXY, peerId);
            vertx.eventBus().publish(Addresses.EVENT_SOCKET_CLOSED, peerId);
            vertx.undeploy(deploymentID(), result -> {
                if (result.failed()) {
                    LOGGER.warn("failed to un-deploy: {}", result.cause().getMessage());
                }
            });
        };
    }
}
