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



    @Override
    public void start(Future<Void> future) {

        if (config().isEmpty()) {
            future.fail("no config found. apply a json config file via --conf param");
            vertx.close();
            return;
        }
        LOGGER.debug("config: {}", config().encode());
        vertx.deployVerticle(
                DtnApiHandler.class.getName(),
                new DeploymentOptions().setConfig(config().getJsonObject("api")),
                primary -> {
                    if (primary.failed()) {
                        LOGGER.error("deployment of primary verticle {} failed: {}",
                                DtnApiHandler.class.getName(),
                                primary.cause().getMessage()
                                );
                        vertx.close();
                        return;
                    }
                    deployDependendVerticles(config());
                }
                );
        // TODO: build some kind of supervisor who monitors the created verticles and restarts them on failure

    }

    private void deployDependendVerticles(JsonObject config) {
        final Map<Class, DeploymentOptions> verticles = new HashMap<>();

        verticles.put(DciListener.class, new DeploymentOptions().setConfig(config.getJsonObject("dci")));
        verticles.put(DataProviderListener.class, new DeploymentOptions().setConfig(config.getJsonObject("dem")));
        verticles.put(MessageForwarder.class, new DeploymentOptions());
        verticles.put(DataReceiverSupervisor.class, new DeploymentOptions());

        for (Class verticle : verticles.keySet()) {
            LOGGER.debug("deploying verticle " + verticle.getName());
            vertx.deployVerticle(verticle.getName(), verticles.get(verticle), deployment -> {
                if (deployment.succeeded()) {
                    LOGGER.info("deployment of {} succeeded.", verticle.getName());
                }
                else {
                    LOGGER.warn("deployment of {} failed: {}", verticle.getName(), deployment.cause().getMessage());
                }
            });
        }
    }
}
