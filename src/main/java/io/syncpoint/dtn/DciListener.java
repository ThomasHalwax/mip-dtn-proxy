package io.syncpoint.dtn;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.datagram.DatagramSocket;
import io.vertx.core.datagram.DatagramSocketOptions;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.XML;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.*;

public final class DciListener extends AbstractVerticle {

    static final Logger LOGGER = LoggerFactory.getLogger(DciListener.class);
    static final int DCI_PORT = 13152;

    private final Set<String> myIpAddresses = getLocalIpAddresses();

    private final Map<String,String> localDemInstances = new HashMap<>();

    private DatagramSocket listeningSocket;
    private DatagramSocket sendingSocket;

    @Override
    public void start() {

        DatagramSocketOptions listeningSocketOptions = new DatagramSocketOptions();
        listeningSocket = vertx.createDatagramSocket(listeningSocketOptions);

        listeningSocket.listen(DCI_PORT, "0.0.0.0", instance -> {
           if (instance.succeeded()) {
               LOGGER.debug("dci listener listens on {}", listeningSocket.localAddress().toString());

               listeningSocket.handler(datagramPacket -> {

                   if (myIpAddresses.contains(datagramPacket.sender().host())) {
                       LOGGER.debug("ignoring broadcast from myself");
                       //TODO: turn on ignoring dci from self
                       //return;
                   }
                   LOGGER.info("received dci on local network");

                   String xmlDci = datagramPacket.data().toString();
                   JSONObject dci;
                   String dciScope;
                   try {
                       dci = XML.toJSONObject(xmlDci);
                       dciScope = dci.getJSONObject("DCI").getString("DciScope");
                   }
                   catch (JSONException jsonParserException) {
                       LOGGER.warn("unable to parse dci", jsonParserException.getMessage());
                       return;
                   }

                   if ("ANNOUNCE".equals(dciScope)) {
                       // DCI announcement from a locally connected DEM
                       LOGGER.debug("DCI ANNOUNCE");
                       vertx.eventBus().publish(Addresses.EVENT_DCI_ANNOUNCED, dci.toString());
                   }
                   else if ("REPLY".equals(dciScope)) {
                       // DCI reply from a locally connected DEM
                       // which is the answer to an announcement sent previously
                       LOGGER.debug("DCI REPLY");
                       vertx.eventBus().publish(Addresses.EVENT_DCI_REPLYED, dci.toString());
                   }
                   else {
                       LOGGER.debug("INVALID DCI? {}", xmlDci);
                   }
                   final int nodeID = dci.getJSONObject("DCI").getJSONObject("DciBody").getInt("NodeID");
                   localDemInstances.put(String.valueOf(nodeID), datagramPacket.sender().host());
                   LOGGER.debug("added {} to the local dem instances", nodeID);
                   LOGGER.debug("{} local dem instances", localDemInstances.size());
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

        // consume remote replies and create a listening DEM proxy server for each remote DEM
        // host and port of the local dem proxy will replace the remote values
        vertx.eventBus().localConsumer(Addresses.COMMAND_REPLY_DCI, message -> {
            // TODO: modify received DCI and send reply message to local network

        });


    }

    private void broadcastDci(Buffer dciBuffer) {
        sendingSocket.send(dciBuffer, DCI_PORT, "255.255.255.255", broadcastHandler -> {
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
