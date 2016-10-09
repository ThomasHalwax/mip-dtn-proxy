package io.syncpoint.dtn;

import io.syncpoint.dtn.api.ApiStatusResponse;
import io.syncpoint.dtn.api.StatusCode;
import io.syncpoint.dtn.connection.State;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.logging.LoggerFactory;

public final class DciDtnForwarder extends AbstractConnectionVerticle {

    public DciDtnForwarder() {
        super();
        LOGGER = LoggerFactory.getLogger(DciDtnForwarder.class);
    }

    @Override
    public void start() {

        connect();
    }


    @Override
    Handler<Buffer> getSocketHandler() {
        switch (state) {
            case CONNECTED: {
                return buffer -> {
                    LOGGER.debug("received data of length " + buffer.length());
                    LOGGER.debug(buffer.toString());

                    become(State.SWITCHING);
                    dtnSocket.write("protocol extended\n");
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
