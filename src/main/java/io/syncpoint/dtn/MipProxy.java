package io.syncpoint.dtn;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public final class MipProxy extends AbstractVerticle{
    static final Logger LOGGER = LoggerFactory.getLogger(MipProxy.class);

    final Class[] verticles = {
            //LocalDciListener.class,
            //DtnNotificationListener.class,
            ApiManager.class

    };

    @Override
    public void start(Future<Void> future) {
        final JsonObject config = config();

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
