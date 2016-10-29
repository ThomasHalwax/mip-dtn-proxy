package io.syncpoint.dtn;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.net.NetSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class DataProviderProxy extends AbstractVerticle {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataProviderProxy.class);
    private static final String TERMINAL_STRING = "T_ABRT\r\n";
    private final NetSocket clientSocket;

    public DataProviderProxy(NetSocket clientSocket) {
        this.clientSocket = clientSocket;
    }

    @Override
    public void start() {

        clientSocket.handler(data -> {
            // TODO: assign local and remote node id based on the T_OPEN Request
            // then proxy local data to remote data and vice versa
            // check if a streaming API connection could solve the re-ordering problem
            LOGGER.debug(">> {}", data.toString());
            String m = data.toString();
            if (TERMINAL_STRING.equals(m)) {
                LOGGER.debug("closing socket");
                clientSocket.end();
            }
        });

        clientSocket.closeHandler(socketClosed -> {
            LOGGER.debug("socket was closed, undeploying handler verticle");
            vertx.undeploy(deploymentID(), undeploy -> {
                if (undeploy.succeeded()) {
                    LOGGER.info("undeploying succeeded");
                }
                else {
                    LOGGER.warn("undeploying failed", undeploy.cause());
                }
            });
        });
    }
}
