package io.syncpoint.dtn;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.net.NetServer;
import io.vertx.core.net.NetServerOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class DataProviderListener extends AbstractVerticle {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataProviderListener.class);
    private static final int DEFAULT_PORT = 13152;
    private final int listenerPort;

    public DataProviderListener(int listenerPort) {
        this.listenerPort = listenerPort;
    }
    public DataProviderListener() {
        this(DEFAULT_PORT);
    }

    @Override
    public void start(Future<Void> startup) {
        NetServerOptions options = new NetServerOptions();
        NetServer listener = vertx.createNetServer(options);
        listener.connectHandler(connectedSocket -> {

            LOGGER.debug("connection established from {}:{}", connectedSocket.localAddress().host(), connectedSocket.localAddress().host());

            DataProviderProxy providerProxy = new DataProviderProxy(connectedSocket);
            vertx.deployVerticle(providerProxy);
        });

        listener.listen(this.listenerPort, "0.0.0.0", instance -> {
            if (instance.succeeded()) {
                LOGGER.info("DataProviderListener ready");
                startup.complete();
            }
            else {
                startup.fail(instance.cause());
            }
        });
    }

}
