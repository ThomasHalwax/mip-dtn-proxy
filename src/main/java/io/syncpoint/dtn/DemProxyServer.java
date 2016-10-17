package io.syncpoint.dtn;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.net.NetServer;
import io.vertx.core.net.NetServerOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class DemProxyServer extends AbstractVerticle {

    private static final Logger LOGGER = LoggerFactory.getLogger(DemProxyServer.class);

    private NetServer proxy;
    private String nodeId;
    private String supervisorAddress;

    @Override
    public void start(Future<Void> startup) {
        initialize();

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

    private void initialize() {
        nodeId = config().getString("nodeId");
        if (nodeId == null || nodeId.length() != 9 || (!isNumeric(nodeId))) {
            throw new RuntimeException("invalid nodeId");
        }
        supervisorAddress = config().getString("supervisorAddress");
        if (supervisorAddress == null || supervisorAddress.length() == 0) {
            throw new RuntimeException("supervisorAddress is invalid");
        }
    }

    private boolean isNumeric(String s) {
        return s.matches("[-+]?\\d*\\.?\\d+");
    }

}
