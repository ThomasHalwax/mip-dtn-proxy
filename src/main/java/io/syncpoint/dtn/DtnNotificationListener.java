package io.syncpoint.dtn;

import io.syncpoint.dtn.api.ApiStatusResponse;
import io.syncpoint.dtn.api.StatusCode;
import io.syncpoint.dtn.connection.State;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.parsetools.RecordParser;

public final class DtnNotificationListener extends AbstractConnectionVerticle {

    private JsonObject dtnEvent = new JsonObject();

    public DtnNotificationListener() {
        super();
        LOGGER = LoggerFactory.getLogger(DtnNotificationListener.class);
        registerStateHandler();
    }

    @Override
    public void start() {
        connect();
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

    private void registerStateHandler() {

        addHandler(State.CONNECTED, buffer -> {
            LOGGER.debug("received data of length " + buffer.length());
            LOGGER.debug(buffer.toString());

            become(State.SWITCHING);
            dtnSocket.write("protocol event\n");
        });


        addHandler(State.READY, buffer -> {
            //LOGGER.debug("received data of length " + buffer.length());
            //LOGGER.debug(buffer.toString());
            dtnEventParser.handle(buffer);
        });
    }

    private RecordParser dtnEventParser = RecordParser.newDelimited("\n", token -> {

        String tokenLine = token.toString();
        if (tokenLine == null || tokenLine.length() == 0) {
            LOGGER.debug("event done");
            LOGGER.debug(dtnEvent.toString());
            vertx.eventBus().send("event://dtn.bundle.received", dtnEvent);
            dtnEvent = new JsonObject();
            return;
        }

        int firstDelimiterPos = tokenLine.indexOf(":");
        dtnEvent.put(tokenLine.substring(0, firstDelimiterPos), tokenLine.substring(firstDelimiterPos + 2));

    });



}
