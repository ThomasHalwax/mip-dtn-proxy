package io.syncpoint.dtn;

import io.syncpoint.dtn.connection.State;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.net.NetClient;
import io.vertx.core.net.NetClientOptions;
import io.vertx.core.net.NetSocket;

public abstract class AbstractConnectionVerticle extends AbstractVerticle{

    final String apiHost = "192.168.168.27";
    final int apiPort = 4550;

    State state = State.DISCONNECTED;
    Logger LOGGER;
    NetSocket dtnSocket;

    /**
     * connects to the given api host and port
     * the initial state is DISCONNECTED, after the connection is
     * established, the connection state will be CONNECTED
     */
    void connect()
    {
        JsonObject config = config();
        LOGGER.debug("willing to connect to dtn api at " + config().getString("dtn.api.host"));

        NetClientOptions options = new NetClientOptions().setConnectTimeout(1000);
        NetClient dtnApiClient = vertx.createNetClient(options);
        dtnApiClient.connect(apiPort, apiHost, connectionResult -> {
            if (connectionResult.failed()) {
                LOGGER.error("failed to connect to dtn api", connectionResult.cause());

                vertx.undeploy(this.deploymentID(), done -> {
                    if (done.succeeded()) {
                        LOGGER.warn("successfully undeployed " + this.getClass().getName());
                    }
                });
                return;
            }
            LOGGER.info("connected to dtn api");
            dtnSocket = connectionResult.result();
            become(State.CONNECTED);
        });
    }


    void become(State newState) {
        state = newState;
        dtnSocket.handler(getSocketHandler());
    }

    abstract Handler<Buffer> getSocketHandler();

}
