package io.syncpoint.dtn;

import io.syncpoint.dtn.api.ApiStatusResponse;
import io.syncpoint.dtn.api.StatusCode;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.net.NetClient;
import io.vertx.core.net.NetClientOptions;
import io.vertx.core.net.NetSocket;

public final class DtnNotificationListener extends AbstractVerticle {

    static final Logger LOGGER = LoggerFactory.getLogger(DtnNotificationListener.class);

    final String apiHost = "192.168.168.27";
    final int apiPort = 4550;

    private enum State {
        DISCONNECTED,
        CONNECTED,
        SWITCHING,
        READY
    }

    private NetSocket dtnSocket;
    private State state = State.DISCONNECTED;

    @Override
    public void start() {
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

    @Override
    public void stop() {
        if (dtnSocket != null) {
            dtnSocket.end();
            dtnSocket.close();
            LOGGER.debug("closed dtn socket");
        }
        LOGGER.warn(this.getClass().getName() + " was stopped");
    }

    private void become(State newState) {
        LOGGER.debug("transition from " + this.state + " to " + newState);
        this.state = newState;
        this.dtnSocket.handler(getSocketHandler());
    }

    private Handler<Buffer> getSocketHandler() {
        switch (this.state) {
            case CONNECTED: {
                return buffer -> {
                    LOGGER.debug("received data of length " + buffer.length());
                    LOGGER.debug(buffer.toString());

                    become(State.SWITCHING);
                    dtnSocket.write("protocol event\n");
                };
            }
            case SWITCHING: {
                return buffer -> {
                    LOGGER.debug("received data of length " + buffer.length());
                    LOGGER.debug(buffer.toString());

                    ApiStatusResponse response = ApiStatusResponse.parse(buffer.toString());
                    if (StatusCode.API_STATUS_OK == response.getCode()) {
                        become(State.READY);
                    }
                    else {
                        LOGGER.warn("switching command failed: " + response);
                    }
                };
            }
            case READY: {
                return buffer -> {
                    LOGGER.debug("received data of length " + buffer.length());
                    LOGGER.debug(buffer.toString());
                };
            }
            default: {
                return buffer -> {
                    LOGGER.debug("received data of length " + buffer.length());
                    LOGGER.debug(buffer.toString());
                };
            }
        }
    }

}
