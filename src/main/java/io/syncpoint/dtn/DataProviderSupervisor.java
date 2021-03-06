package io.syncpoint.dtn;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.net.NetServer;
import io.vertx.core.net.NetServerOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class DataProviderSupervisor extends AbstractVerticle {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataProviderSupervisor.class);
    private static final int DEFAULT_PORT = 13152;
    private final int listenerPort;

    public DataProviderSupervisor(int listenerPort) {
        this.listenerPort = listenerPort;
    }
    public DataProviderSupervisor() {
        this(DEFAULT_PORT);
    }

    @Override
    public void start(Future<Void> startup) {
        NetServerOptions options = new NetServerOptions();
        NetServer listener = vertx.createNetServer(options);
        listener.connectHandler(connectedSocket -> {

            LOGGER.debug("connection established from {}:{}",
                    connectedSocket.remoteAddress().host(),
                    connectedSocket.remoteAddress().port());

            // we need to deploy a new verticle
            // this will take some time so we disable auto read
            connectedSocket.pause();
            DataProviderProxy providerProxy = new DataProviderProxy(connectedSocket);
            vertx.deployVerticle(providerProxy);
        });

        listener.listen(this.listenerPort, "0.0.0.0", instance -> {
            if (instance.succeeded()) {
                LOGGER.info("DataProviderSupervisor listening on port {}", instance.result().actualPort());
                startup.complete();
            }
            else {
                startup.fail(instance.cause());
            }
        });
    }

}
