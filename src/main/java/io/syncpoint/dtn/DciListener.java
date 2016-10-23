package io.syncpoint.dtn;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.datagram.DatagramSocket;
import io.vertx.core.datagram.DatagramSocketOptions;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Base64;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

public final class DciListener extends AbstractVerticle {

    static final Logger LOGGER = LoggerFactory.getLogger(DciListener.class);
    static final int DCI_PORT = 13152;

    private final Set<String> myIpAddresses = getLocalIpAddresses();

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
                   if (myIpAddresses.contains(datagramPacket.sender().host())) {
                       LOGGER.debug("ignoring broadcast from myself");
                       return;
                   }
                   LOGGER.info("received dci on local network");

                   String dci = datagramPacket.data().toString();
                   DeliveryOptions dciDeliveryOptions = new DeliveryOptions();
                   if (dci.contains("ANNOUNCE")) {
                       // DCI announcement from a locally connected DEM
                       LOGGER.debug("DCI ANNOUNCE");
                       //localDemInstances.add(datagramPacket.sender().host());
                       //TODO: set header value to indicate broadcast
                       //dciDeliveryOptions.addHeader("ENDPOINT", "GROUP");
                       vertx.eventBus().publish(Addresses.EVENT_DCI_ANNOUNCED, dci, dciDeliveryOptions);
                   }
                   else if (dci.contains("REPLY")) {
                       // DCI reply from a locally connected DEM
                       // which is the answer to an announcement sent previously
                       LOGGER.debug("DCI REPLY");
                       dciDeliveryOptions.addHeader("ENDPOINT", "SINGLETON");
                       vertx.eventBus().publish(Addresses.EVENT_DCI_REPLYED, dci, dciDeliveryOptions);
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

        // consume remote announcements (which are sent to local),
        // modify DCI content and broadcast message to DCI listening port
        // target of the message are all local dem instances
        vertx.eventBus().localConsumer(Addresses.COMMAND_ANNOUNCE_DCI, message -> {
            final byte[] rawMessage = Base64.getDecoder().decode(message.body().toString());
            Buffer b = Buffer.buffer(rawMessage);
            broadcastDci(b);
        });

        // consume remote replies and create a listening dem proxy server for each remote dem
        // host and port of the local dem proxy will replace the remote values
        vertx.eventBus().localConsumer(Addresses.COMMAND_REPLY_DCI, message -> {
            // TODO: modify received DCI and send reply message to local network

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

    private void broadcastDci(Buffer dciBuffer) {
        sendingSocket.send(dciBuffer, 13152, "255.255.255.255", broadcastHandler -> {
            if (broadcastHandler.succeeded()) {
                LOGGER.debug("broadcasted DCI");
            }
            else {
                LOGGER.warn("failed to broadcast DIC: {}", broadcastHandler.cause().toString());
            }
        });
    }


    private Set<String> getLocalIpAddresses() {
        Set<String> ipList = new HashSet<>();
        try {
            final Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            while(networkInterfaces.hasMoreElements())
            {
                NetworkInterface networkInterface = networkInterfaces.nextElement();
                Enumeration ipAddresses = networkInterface.getInetAddresses();
                while (ipAddresses.hasMoreElements())
                {
                    InetAddress i = (InetAddress) ipAddresses.nextElement();
                    ipList.add(i.getHostAddress());
                }
            }
        } catch (SocketException networkException) {
            LOGGER.error("failed to enumerate network interfaces", networkException);
        }

        return ipList;
    }
}
