package io.syncpoint.dtn;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.net.NetServer;
import io.vertx.core.net.NetServerOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class DemProxyServer extends AbstractVerticle {

    private static final Logger LOGGER = LoggerFactory.getLogger(DemProxyServer.class);
    private final String remoteDci;

    private NetServer proxy;
    private String nodeId;
    private String supervisorAddress;

    public DemProxyServer(String remoteDci, String supervisorAddress) {

        this.remoteDci = remoteDci;
        this.supervisorAddress = supervisorAddress;
    }

    @Override
    public void start(Future<Void> startup) {
        NetServerOptions options = new NetServerOptions();
        vertx.createNetServer(options).listen(instance -> {
            if (instance.succeeded()) {
                proxy = instance.result();
                startup.complete();
                String proxyInfo = nodeId + "=" + proxy.actualPort();
                LOGGER.debug("proxy instance for {}", proxyInfo);
                vertx.eventBus().send(supervisorAddress, proxyInfo);
            }
        });
    }

}
