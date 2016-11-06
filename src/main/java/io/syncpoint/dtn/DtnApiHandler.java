package io.syncpoint.dtn;

import io.syncpoint.dtn.api.ApiMessage;
import io.syncpoint.dtn.api.ApiResponse;
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

import java.util.LinkedList;
import java.util.Queue;
import java.util.function.Consumer;

public final class DtnApiHandler extends AbstractVerticle {

    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(DtnApiHandler.class);
    private static final String INITIAL_API_MESSAGE = "IBR-DTN 1.0.1 (build 1da5501) API 1.0.1";

    private String nodeName = "";
    private NetSocket dtnSocket;
    private Queue<ApiResponse> apiResponses = new LinkedList<>();
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
                send("nodename", expectedNodenameResponse());
                send("registration add " + Addresses.DTN_DCI_ANNOUNCE_ADDRESS);
                send("registration add " + Addresses.DTN_DCI_REPLY_ADDRESS);
                send("endpoint add " + Addresses.DTN_REPORT_TO_ADDRESS);
                started.complete();
            }
            else {
                LOGGER.warn("connect failed ", attempt.cause());
                started.fail(attempt.cause());
            }
        });

        // vert.x eventbus listeners
        vertx.eventBus().localConsumer(Addresses.COMMAND_SEND_BUNDLE, bundleMessage -> {
            JsonObject bundle = (JsonObject) bundleMessage.body();
            send(BundleSerializer.serialize(bundle));
            LOGGER.debug("sent bundle");
        });

        vertx.eventBus().localConsumer(Addresses.COMMAND_REGISTER_PROXY, localNodeAddress -> {
            String localDPProxyAddress = localNodeAddress.body().toString();
            send("registration add " + localDPProxyAddress);
            LOGGER.debug("added registration for local DP proxy {}", localDPProxyAddress);
        });

        vertx.eventBus().localConsumer(Addresses.COMMAND_UNREGISTER_PROXY, localNodeAddress -> {
            String localDPProxyAddress = localNodeAddress.body().toString();
            send("registration del " + localDPProxyAddress);
            LOGGER.debug("removed registration for local DP proxy {}", localDPProxyAddress);
        });

        vertx.eventBus().localConsumer(Addresses.QUERY_NODENAME, message -> {
            LOGGER.debug("sending nodename back to {}", message.replyAddress());
            message.reply(this.nodeName);
        });
    }

    // handle response to the command "nodename"
    private ApiResponse expectedNodenameResponse() {
        ApiResponse response = new ApiResponse(StatusCode.OK);
        response.successHandler(m -> {
            this.nodeName = m.getMessage().replace("200 NODENAME ", "");
            LOGGER.debug("DTN node name: {}", this.nodeName);
        });
        return response;
    }

    // handle STATUS response from API
    private void handleApiMessage(String message) {
        LOGGER.debug(message);

        final ApiMessage apiMessage = ApiMessage.parse(message);
        if (apiMessage.getCode() == StatusCode.NOTIFY_BUNDLE) {
            send("bundle load queue");
            send("bundle get");
            send("bundle free");
        }
        else {
            final ApiResponse apiResponse = apiResponses.remove();
            apiResponse.accept(apiMessage);
        }
    }

    // handling non STATUS response from the API
    private void handleData(String message) {
        LOGGER.debug(message);
        if (INITIAL_API_MESSAGE.equals(message)) return;

        bundleParser.addData(message);
        if (bundleParser.done()) {
            vertx.eventBus().publish(Addresses.EVENT_BUNDLE_RECEIVED, bundleParser.getBundle());
            bundleParser.reset();
        }
    }

    // some variants to send a command/query to the API
    private void send(String message) {
        final ApiResponse expectedApiResponse = new ApiResponse(StatusCode.OK);
        expectedApiResponse.successHandler(apiSuccessHandler());
        expectedApiResponse.failHandler(apiErrorHandler());
        send(message, expectedApiResponse);
    }

    private void send(String message, ApiResponse expectedResponse) {
        LOGGER.debug(message);
        apiResponses.add(expectedResponse);
        dtnSocket.write(message + "\n");
    }

    private void send(Buffer buffer) {
        dtnSocket.write(buffer);
    }

    // default consumers used for checking the success/failure of an API command
    private Consumer<ApiMessage> apiSuccessHandler() {
        return apiMessage -> {
        };
    }

    private Consumer<ApiMessage> apiErrorHandler() {
        return apiMessage -> {
            LOGGER.warn("message received does not match expected one: {}", apiMessage.getMessage());
        };
    }
}