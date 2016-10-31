package io.syncpoint.dtn;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.datagram.DatagramSocket;
import io.vertx.core.datagram.DatagramSocketOptions;
import io.vertx.core.eventbus.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.Inet6Address;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Base64;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public final class DciListener extends AbstractVerticle {

    public static final String DEFAULT_DCI_HOST = "0.0.0.0";
    public static final int DEFAULT_DCI_PORT = 13152;
    private static final Logger LOGGER = LoggerFactory.getLogger(DciListener.class);

    // KEY = IP Address, VALUES = Broadcast Address
    private final Map<String, String> myIpAddresses = getLocalIpAddresses();

    private final Pattern dciReplicationNodeIPAddressPattern = Pattern.compile("<ReplicationNodeIPAddress>(?:[0-9]{1,3}\\.){3}[0-9]{1,3}</ReplicationNodeIPAddress>");
    private final Pattern dciReplicationNodePortPattern = Pattern.compile("<ReplicationNodePort>\\d+</ReplicationNodePort>");

    private DatagramSocket listeningSocket;
    private DatagramSocket sendingSocket;

    @Override
    public void start() {

        DatagramSocketOptions listeningSocketOptions = new DatagramSocketOptions();
        listeningSocketOptions.setBroadcast(true);
        listeningSocket = vertx.createDatagramSocket(listeningSocketOptions);

        listeningSocket.listen(
                config().getInteger("port", DEFAULT_DCI_PORT),
                config().getString("host", DEFAULT_DCI_HOST), instance -> {
           if (instance.succeeded()) {
               LOGGER.debug("dci listener listens on {}", listeningSocket.localAddress().toString());

               listeningSocket.handler(datagramPacket -> {
                   if (myIpAddresses.keySet().contains(datagramPacket.sender().host())) {
                       LOGGER.debug("ignoring broadcast from myself");
                       return;
                   }
                   LOGGER.info("received dci on local network");

                   String xmlDci = datagramPacket.data().toString();
                   if (xmlDci.length() < 250) return;

                   if (xmlDci.contains("ANNOUNCE")) {
                       // DCI announcement from a locally connected DEM
                       LOGGER.debug("DCI ANNOUNCE");
                       vertx.eventBus().publish(Addresses.EVENT_DCI_ANNOUNCED, xmlDci);
                   }
                   else if (xmlDci.contains("REPLY")) {
                       // DCI reply from a locally connected DEM
                       // which is the answer to an announcement sent previously
                       LOGGER.debug("DCI REPLY");
                       vertx.eventBus().publish(Addresses.EVENT_DCI_REPLYED, xmlDci);
                   }
                   else {
                       LOGGER.debug("INVALID DCI? {}", xmlDci);
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

        //sendingSocket = vertx.createDatagramSocket(sendingSocketOptions);

        // modify DCI content and broadcast message to DCI listening port
        // target of the message are all local dem instances
        vertx.eventBus().localConsumer(Addresses.COMMAND_ANNOUNCE_DCI, this::handleRemoteDciMessage);

        // consume remote replies
        // since we don't have an correlation information we need to broadcast
        // replies, too
        vertx.eventBus().localConsumer(Addresses.COMMAND_REPLY_DCI, this::handleRemoteDciMessage);
    }

    private void handleRemoteDciMessage(Message<Object> message) {
        String remoteDci = new String(Base64.getDecoder().decode((String)message.body()));

        String proxyDci = dciReplicationNodeIPAddressPattern.matcher(remoteDci)
                .replaceAll("<ReplicationNodeIPAddress>" + config().getString("host") + "</ReplicationNodeIPAddress>");
        proxyDci =  dciReplicationNodePortPattern.matcher(proxyDci)
                .replaceAll("<ReplicationNodePort>" + String.valueOf(DEFAULT_DCI_PORT) + "</ReplicationNodePort>");

        broadcastDci(Buffer.buffer(proxyDci));
    }

    private void broadcastDci(Buffer dciBuffer) {

        listeningSocket.send(dciBuffer, DEFAULT_DCI_PORT, myIpAddresses.get(config().getString("host")), broadcastHandler -> {
            if (broadcastHandler.succeeded()) {
                LOGGER.debug("broadcasted DCI");
            }
            else {
                LOGGER.warn("failed to broadcast DCI: {}", broadcastHandler.cause().toString());
            }
        });
    }

    private Map<String, String> getLocalIpAddresses() {
        Map<String, String> localAddresses = new HashMap<>();
        Predicate<InterfaceAddress> isNotLoopback = interfaceAddress -> !interfaceAddress.getAddress().isLoopbackAddress();
        Predicate<InterfaceAddress> isIPv4 = interfaceAddress -> !(interfaceAddress.getAddress() instanceof Inet6Address);

        try {
            final Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            while(networkInterfaces.hasMoreElements())
            {
                NetworkInterface networkInterface = networkInterfaces.nextElement();
                networkInterface.getInterfaceAddresses().stream()
                        .filter(isNotLoopback)
                        .filter(isIPv4)
                        .forEach(ifaceAddress -> localAddresses.put(
                                ifaceAddress.getAddress().getHostAddress(),
                                ifaceAddress.getBroadcast().getHostAddress()));
            }
        } catch (SocketException networkException) {
            LOGGER.error("failed to enumerate network interfaces", networkException);
        }
        return localAddresses;
    }
}
