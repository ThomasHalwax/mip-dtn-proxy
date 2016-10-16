package io.syncpoint.dtn;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.datagram.DatagramSocket;
import io.vertx.core.datagram.DatagramSocketOptions;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public final class LocalDciListener extends AbstractVerticle {
    static final Logger LOGGER = LoggerFactory.getLogger(LocalDciListener.class);
    static final int DCI_PORT = 13152;
    private DatagramSocket socket;

    @Override
    public void start() {
        LOGGER.debug("starting ...");
        DatagramSocketOptions socketOptions = new DatagramSocketOptions();
        socket = vertx.createDatagramSocket(socketOptions);
        socket.listen(DCI_PORT, "0.0.0.0", listenOnSocketResult -> {
           if (listenOnSocketResult.succeeded()) {
               LOGGER.debug("listening ...");
               socket.handler(datagramPacket -> LOGGER.info("received dci"));
           }
           else {
               LOGGER.error("failed to listen", listenOnSocketResult.cause());
               LOGGER.warn("undeploying self");
               vertx.undeploy(deploymentID());
           }
        });
    }

    @Override
    public void stop() {
        if (socket != null) {
            socket.close(closed -> {
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
