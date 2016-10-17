package io.syncpoint.dtn;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public final class MipProxy extends AbstractVerticle{
    static final Logger LOGGER = LoggerFactory.getLogger(MipProxy.class);

    final Class[] verticles = {
            DciListener.class,
            //DtnNotificationListener.class,
            DtnApiHandler.class

    };

    @Override
    public void start(Future<Void> future) {
        final JsonObject config = config();

        // TODO: build some kind of supervisor who monitors the created verticles and restarts them on failure

        for (Class verticle : verticles) {
            LOGGER.debug("deploying verticle " + verticle.getName());
            vertx.deployVerticle(verticle.getName(), deployment -> {
                if (deployment.succeeded()) {
                    LOGGER.info("verticle deployment succeeded: " + deployment.result());
                }
                else {
                    LOGGER.warn("failed to deploy verticle: " + deployment.cause());
                }
            });
        }
    }
}
