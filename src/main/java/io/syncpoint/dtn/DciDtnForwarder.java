package io.syncpoint.dtn;

import io.syncpoint.dtn.connection.State;
import io.vertx.core.logging.LoggerFactory;

public final class DciDtnForwarder extends AbstractConnectionVerticle {

    public DciDtnForwarder() {
        super();
        LOGGER = LoggerFactory.getLogger(DciDtnForwarder.class);

        addHandler(State.CONNECTED, buffer -> {
            LOGGER.debug("received data of length " + buffer.length());
            LOGGER.debug(buffer.toString());

            become(State.SWITCHING);
            dtnSocket.write("protocol extended\n");
        });
    }

    @Override
    public void start() {
        connect();
    }

}
