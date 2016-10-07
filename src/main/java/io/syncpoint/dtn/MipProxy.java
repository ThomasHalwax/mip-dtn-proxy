package io.syncpoint.dtn;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public final class MipProxy extends AbstractVerticle{
    static final Logger LOGGER = LoggerFactory.getLogger(MipProxy.class);
    @Override
    public void start(Future<Void> future) {
        vertx.deployVerticle(LocalDciListener.class.getCanonicalName(), dciListenerDeployed -> {
            if (dciListenerDeployed.failed()) {
                LOGGER.warn("failed to deploy verticle", dciListenerDeployed.cause());
                vertx.close();
            }
            else {
                LOGGER.info("dci listener up and running");
            }

        });

        vertx.deployVerticle(DtnNotificationListener.class.getCanonicalName(), deploymentResult -> {
            if (deploymentResult.failed()) {
                LOGGER.warn("failed to deploy verticle", deploymentResult.cause());
                vertx.close();
            }
            else {
                LOGGER.info("dtn notification listener up and running");
            }

        });
    }
}
