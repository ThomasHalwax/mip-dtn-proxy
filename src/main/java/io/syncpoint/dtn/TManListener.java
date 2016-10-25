package io.syncpoint.dtn;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.net.NetServer;
import io.vertx.core.net.NetServerOptions;
import io.vertx.core.net.NetSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class TManListener extends AbstractVerticle {

    private static final Logger LOGGER = LoggerFactory.getLogger(TManListener.class);
    private static final int DEFAULT_PORT = 13151;
    private final int listenerPort;

    public TManListener(int listenerPort) {
        this.listenerPort = listenerPort;
    }
    public TManListener() {
        this(DEFAULT_PORT);
    }

    @Override
    public void start(Future<Void> startup) {
        NetServerOptions options = new NetServerOptions();
        NetServer tManListener = vertx.createNetServer(options);
        tManListener.connectHandler(connectedSocket -> {

            LOGGER.debug("connection established from {}:{}", connectedSocket.localAddress().host(), connectedSocket.localAddress().host());

            PassiveTManHandler socketHandler = new PassiveTManHandler(connectedSocket);
            vertx.deployVerticle(socketHandler);
        });

        tManListener.listen(this.listenerPort, "0.0.0.0", instance -> {
            if (instance.succeeded()) {
                LOGGER.info("TManListener ready");
                startup.complete();
            }
            else {
                startup.fail(instance.cause());
            }
        });
    }

}
