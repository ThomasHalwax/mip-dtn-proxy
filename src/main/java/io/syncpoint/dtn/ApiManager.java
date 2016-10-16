package io.syncpoint.dtn;

import io.syncpoint.dtn.api.ApiMessage;
import io.syncpoint.dtn.api.StatusCode;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.NetClientOptions;
import io.vertx.core.net.NetSocket;
import io.vertx.core.parsetools.RecordParser;
import org.slf4j.Logger;

public final class ApiManager extends AbstractVerticle {
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(ApiManager.class);
    private static final String DTN_HOST = "172.16.125.133";
    private static final String DTN_REGISTRATION_ADDRESS = "dtn://tolerator/dem/dci";



    private NetSocket dtnSocket;
    private JsonObject currentBundle = new JsonObject();



    @Override
    public void start() {

        NetClientOptions options = new NetClientOptions();
        options.setTcpKeepAlive(true);

        vertx.createNetClient(options).connect(4550, DTN_HOST, attempt -> {
            if (attempt.succeeded()) {
                LOGGER.debug("connected");
                dtnSocket = attempt.result();
                RecordParser recordParser = RecordParser.newDelimited("\n", apiResponse -> {
                    String message = apiResponse.toString();
                    if (ApiMessage.isParseable(message)) {
                        handleApiMessage(message);
                    }
                    else {
                        handleData(message);
                    }
                });
                dtnSocket.handler(buffer -> {
                    recordParser.handle(buffer);
                });
                send("protocol extended");
                send("registration add " + DTN_REGISTRATION_ADDRESS);
            }
            else {
                LOGGER.warn("connect failed ", attempt.cause());
            }
        });
    }

    @Override
    public void stop() {
        dtnSocket.handler(buffer -> {
            LOGGER.debug(buffer.toString());
        });

        send("registration del " + DTN_REGISTRATION_ADDRESS);
        send("exit");

        dtnSocket.end();
        dtnSocket.close();
        LOGGER.debug("socket closed");
    }


    private void handleApiMessage(String message) {
        LOGGER.debug(message);
        final ApiMessage apiMessage = ApiMessage.parse(message);
        if (apiMessage.getCode() == StatusCode.OK) return;
        if (apiMessage.getCode() == StatusCode.NOTIFY_BUNDLE) {
            send("bundle load queue");
            send("bundle get");
            send("bundle free");
        }
    }

    private void handleData(String message) {
        LOGGER.debug(message);
    }

    private void send(String request) {
        LOGGER.debug(request);
        dtnSocket.write(request + "\n");
    }


}
