package io.syncpoint.dtn;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.datagram.DatagramSocket;
import io.vertx.core.datagram.DatagramSocketOptions;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

public final class DciListener extends AbstractVerticle {

    static final Logger LOGGER = LoggerFactory.getLogger(DciListener.class);
    static final int DCI_PORT = 13152;

    private final Set<String> localDemInstances = new HashSet<>();

    private DatagramSocket listeningSocket;
    private DatagramSocket sendingSocket;

    @Override
    public void start() {
        LOGGER.debug("starting ...");

        DatagramSocketOptions listeningSocketOptions = new DatagramSocketOptions();
        listeningSocket = vertx.createDatagramSocket(listeningSocketOptions);

        listeningSocket.listen(DCI_PORT, "0.0.0.0", instance -> {
           if (instance.succeeded()) {
               LOGGER.debug("dci listener listens on {}", listeningSocket.localAddress().toString());
               listeningSocket.handler(datagramPacket -> {
                   LOGGER.info("received dci on local network");
                   String dci = datagramPacket.data().toString();
                   DeliveryOptions dciDeliveryOptions = new DeliveryOptions();
                   if (dci.contains("ANNOUNCE")) {
                       // DCI announcement from a locally connected DEM
                       LOGGER.debug("DCI ANNOUNCE");
                       localDemInstances.add(datagramPacket.sender().host());
                       dciDeliveryOptions.addHeader("ENDPOINT", "GROUP");
                       vertx.eventBus().publish("remote://dem/dci/announce", dci, dciDeliveryOptions);
                   }
                   else if (dci.contains("REPLY")) {
                       // DCI reply from a locally connected DEM
                       // which is the answer to an announcement sent previously
                       LOGGER.debug("DCI REPLY");
                       dciDeliveryOptions.addHeader("ENDPOINT", "SINGLETON");
                       vertx.eventBus().publish("remote://dem/dci/reply", dci, dciDeliveryOptions);
                   }
                   else {
                       LOGGER.debug("INVALID DCI? {}", dci);
                   }
               });
           }
           else {
               LOGGER.error("failed to listen", instance.cause());
               LOGGER.warn("undeploying self");
               vertx.undeploy(deploymentID());
           }
        });

        DatagramSocketOptions sendingSocketOptions = new DatagramSocketOptions();
        sendingSocketOptions.setBroadcast(true);
        sendingSocket = vertx.createDatagramSocket(sendingSocketOptions);

        // consume remote announcements, modify DCI content and broadcast message to DCI listening port
        // target of the message are all local dem instances
        vertx.eventBus().localConsumer("local://dem/dci/announce", message -> {

            String messageBody = message.body().toString();
            sendingSocket.send(messageBody, 13152, "255.255.255.255", broadcastHandler -> {
                if (broadcastHandler.succeeded()) {
                    LOGGER.debug("broadcasted DCI");
                }
                else {
                    LOGGER.warn("failed to broadcast DIC: {}", broadcastHandler.cause().toString());
                }
            });
        });

        // consume remote replies and create a listening dem proxy server for each remote dem
        // host and port of the local dem proxy will replace the remote values
        vertx.eventBus().localConsumer("local://dem/dci/reply", message -> {

            String dciReply = message.body().toString();
            DemProxyServer proxy = new DemProxyServer(dciReply, deploymentID() + "/demProxy");
            vertx.deployVerticle(proxy);

        });


    }

    @Override
    public void stop() {
        if (listeningSocket != null) {
            listeningSocket.close(closed -> {
                if (closed.succeeded()) {
                    LOGGER.debug("dci listener was successfully closed");
                }
                else {
                    LOGGER.error("wtf? closing dci listener failed: ", closed.cause());
                }
            });
        }
    }
}
