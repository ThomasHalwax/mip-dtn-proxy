package io.syncpoint.dtn;

import io.syncpoint.dtn.api.ApiStatusResponse;
import io.syncpoint.dtn.api.StatusCode;
import io.syncpoint.dtn.connection.State;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.net.NetClient;
import io.vertx.core.net.NetClientOptions;
import io.vertx.core.net.NetSocket;

import java.util.HashMap;
import java.util.Map;

public abstract class AbstractConnectionVerticle extends AbstractVerticle{

    final String apiHost = "172.16.125.133";
    final int apiPort = 4550;

    State state = State.DISCONNECTED;
    Logger LOGGER;
    NetSocket dtnSocket;

    private Map<State, Handler<Buffer>> handlerRegistry = new HashMap<>();

    public AbstractConnectionVerticle() {

        addHandler(State.SWITCHING, buffer -> {
            LOGGER.debug("received data of length " + buffer.length());
            LOGGER.debug(buffer.toString());

            ApiStatusResponse response = ApiStatusResponse.parse(buffer.toString());
            if (StatusCode.API_STATUS_OK == response.getCode()) {
                become(State.READY);
            }
            else {
                LOGGER.warn("switching command failed: " + response);
            }
        });

        addHandler(State.READY, buffer -> {
            LOGGER.debug("received data of length " + buffer.length());
            LOGGER.debug(buffer.toString());
        });
    }

    void addHandler(State state, Handler<Buffer> handler) {
        handlerRegistry.put(state, handler);
    }

    Handler<Buffer> getHandler(State state) {
        if (! handlerRegistry.containsKey(state)) {
            return buffer -> {
                LOGGER.debug("[DEFAULT HANDLER] received data of length " + buffer.length());
                LOGGER.debug(buffer.toString());
            };
        }

        return handlerRegistry.get(state);
    }

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
        dtnSocket.handler(getHandler(state));
    }

    //abstract Handler<Buffer> getSocketHandler();

}
