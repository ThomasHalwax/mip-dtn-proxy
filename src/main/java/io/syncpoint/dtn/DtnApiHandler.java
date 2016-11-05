package io.syncpoint.dtn;

import io.syncpoint.dtn.api.ApiMessage;
import io.syncpoint.dtn.api.StatusCode;
import io.syncpoint.dtn.bundle.BundleParser;
import io.syncpoint.dtn.bundle.BundleSerializer;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.NetClientOptions;
import io.vertx.core.net.NetSocket;
import io.vertx.core.parsetools.RecordParser;
import org.slf4j.Logger;

public final class DtnApiHandler extends AbstractVerticle {

    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(DtnApiHandler.class);
    private static final String INITIAL_API_MESSAGE = "IBR-DTN 1.0.1 (build 1da5501) API 1.0.1";

    private NetSocket dtnSocket;
    private BundleParser bundleParser = new BundleParser();

    @Override
    public void start(Future<Void> started) {

        NetClientOptions options = new NetClientOptions();
        options.setTcpKeepAlive(true);

        LOGGER.debug("connecting to {}:{}", config().getString("host"), config().getInteger("port"));
        vertx.createNetClient(options).connect(config().getInteger("port"), config().getString("host"), attempt -> {
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
                dtnSocket.handler(recordParser::handle);
                send("protocol extended");
                send("registration add " + Addresses.DTN_DCI_ANNOUNCE_ADDRESS);
                send("registration add " + Addresses.DTN_DCI_REPLY_ADDRESS);
                send("endpoint add " + Addresses.DTN_REPORT_TO_ADDRESS);
                //send("registration list");
                started.complete();
            }
            else {
                LOGGER.warn("connect failed ", attempt.cause());
                started.fail(attempt.cause());
            }
        });

        vertx.eventBus().localConsumer(Addresses.COMMAND_SEND_BUNDLE, bundleMessage -> {
            JsonObject bundle = (JsonObject) bundleMessage.body();
            send(BundleSerializer.serialize(bundle));
            LOGGER.debug("sent bundle");
        });

        vertx.eventBus().localConsumer(Addresses.COMMAND_REGISTER_PROXY, localNodeAddress -> {
            String localDPProxyAddress = localNodeAddress.body().toString();
            send("endpoint add " + localDPProxyAddress);
            LOGGER.debug("added registration for local DP proxy {}", localDPProxyAddress);
        });

        vertx.eventBus().localConsumer(Addresses.COMMAND_UNREGISTER_PROXY, localNodeAddress -> {
            String localDPProxyAddress = localNodeAddress.body().toString();
            send("registration del " + localDPProxyAddress);
            LOGGER.debug("removed registration for local DP proxy {}", localDPProxyAddress);
        });
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

        bundleParser.addData(message);
        if (bundleParser.done()) {
            vertx.eventBus().publish(Addresses.EVENT_BUNDLE_RECEIVED, bundleParser.getBundle());
            bundleParser.reset();
        }
    }

    private void send(String request) {
        LOGGER.debug(request);
        dtnSocket.write(request + "\n");
    }

    private void send(Buffer buffer) {
        dtnSocket.write(buffer);
    }
}