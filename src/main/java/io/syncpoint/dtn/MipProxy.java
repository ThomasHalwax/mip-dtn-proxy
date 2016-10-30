package io.syncpoint.dtn;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;


public final class MipProxy extends AbstractVerticle{
    static final Logger LOGGER = LoggerFactory.getLogger(MipProxy.class);

    final Map<Class, DeploymentOptions> verticles = new HashMap<>();

    @Override
    public void start(Future<Void> future) {

        final JsonObject config = config();
        if (config.isEmpty()) {
            LOGGER.error("no config found. apply a json config file via --conf param");
            future.fail("no config found. apply a json config file via --conf param");
            vertx.close();
            return;
        }
        LOGGER.debug("config: {}", config.encode());

        verticles.put(DciListener.class, new DeploymentOptions());
        verticles.put(DataProviderListener.class, new DeploymentOptions());
        verticles.put(MessageForwarder.class, new DeploymentOptions());
        verticles.put(DtnApiHandler.class, new DeploymentOptions().setConfig(config.getJsonObject("api")));

        // TODO: build some kind of supervisor who monitors the created verticles and restarts them on failure

        for (Class verticle : verticles.keySet()) {
            LOGGER.debug("deploying verticle " + verticle.getName());
            vertx.deployVerticle(verticle.getName(), verticles.get(verticle), deployment -> {
                if (deployment.succeeded()) {
                    LOGGER.info("deployment of {} succeeded.", verticle.getName());
                }
                else {
                    LOGGER.warn("deployment of {} failed: {}", verticle.getName(), deployment.cause());
                }
            });
        }
    }
}
