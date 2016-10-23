package io.syncpoint.dtn;

import io.syncpoint.dtn.api.ApiMessage;
import io.syncpoint.dtn.api.StatusCode;
import io.syncpoint.dtn.bundle.DtnBundleParser;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.net.NetClientOptions;
import io.vertx.core.net.NetSocket;
import io.vertx.core.parsetools.RecordParser;
import org.slf4j.Logger;

public final class DtnApiHandler extends AbstractVerticle {

    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(DtnApiHandler.class);
    private static final String DTN_API_HOST = "172.16.125.133";
    private static final String INITIAL_API_MESSAGE = "IBR-DTN 1.0.1 (build 1da5501) API 1.0.1";

    // other nodes broadcast their DCI announce to this address
    private static final String DTN_DCI_ANNOUNCE_ADDRESS = "dtn://bataillon/dem/dci/announce";

    // this is reply address for DCI reply messages
    private static final String DTN_DCI_REPLY_ADDRESS = "dtn://bataillon/dem/dci/reply";

    private NetSocket dtnSocket;
    private DtnBundleParser currentBundle = new DtnBundleParser();



    @Override
    public void start() {

        NetClientOptions options = new NetClientOptions();
        options.setTcpKeepAlive(true);

        LOGGER.debug("connecting to {}:4550", DTN_API_HOST);
        vertx.createNetClient(options).connect(4550, DTN_API_HOST, attempt -> {
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
                send("registration add " + DTN_DCI_ANNOUNCE_ADDRESS);
                send("registration add " + DTN_DCI_REPLY_ADDRESS);
            }
            else {
                LOGGER.warn("connect failed ", attempt.cause());
                LOGGER.warn("undeploying {} with id {}", DtnApiHandler.class.getName(), deploymentID());
                vertx.undeploy(deploymentID());
            }
        });
    }

    @Override
    public void stop() {
        dtnSocket.handler(buffer -> {
            LOGGER.debug(buffer.toString());
        });

        send("registration del " + DTN_DCI_ANNOUNCE_ADDRESS);
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
        if (INITIAL_API_MESSAGE.equals(message)) return;

        currentBundle.addData(message);
        if (currentBundle.done()) {
            vertx.eventBus().publish("bundle.received", currentBundle.copy());
            currentBundle = new DtnBundleParser();
        }
    }

    private void send(String request) {
        LOGGER.debug(request);
        dtnSocket.write(request + "\n");
    }
}
