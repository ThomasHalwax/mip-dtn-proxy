package io.syncpoint.dtn;

import io.syncpoint.dtn.api.ApiMessage;
import io.syncpoint.dtn.api.StatusCode;
import io.syncpoint.dtn.connection.State;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetSocket;
import io.vertx.core.parsetools.RecordParser;
import org.slf4j.Logger;

import java.util.function.Function;

public final class DtnBundleLoader extends AbstractVerticle {
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(DtnBundleLoader.class);
    private NetSocket dtnSocket;

    public DtnBundleLoader(){
        super();
    }

    @Override
    public void start() {

        Function<String, Future<NetSocket>> connectToApi = host -> {
            Future<NetSocket> f = Future.future();

            vertx.createNetClient().connect(4550, host, attempt -> {
                if (attempt.succeeded()) {
                    LOGGER.debug("connected");
                    NetSocket socket = attempt.result();
                    socket.handler(buffer -> {
                       LOGGER.debug("onConnect: " + buffer.toString());
                        f.complete(attempt.result());
                    });
                    socket.write("\n");

                }
                else {
                    LOGGER.warn("connect failed ", attempt.cause());
                    f.fail(attempt.cause());
                }
            });
            return f;
        };

        Function<NetSocket, Future<NetSocket>> switchProtocol = apiCommand("protocol extended", StatusCode.OK);
        Function<NetSocket, Future<NetSocket>> addRegistration = apiCommand("registration add dtn://tolerator/dem/dci", StatusCode.OK);

        connectToApi
                .apply("172.16.125.133")
                .compose(switchProtocol)
                .compose(addRegistration)
                .setHandler(h -> {
            if (h.succeeded()) {
                LOGGER.debug("becoming a listener ...");
                this.dtnSocket = h.result();
                become(State.READY);
            }
            else {
                LOGGER.warn("failed to connect: ", h.cause());
            }
        });


    }

    private void become(State newState) {
        LOGGER.debug("transition to {}", newState);
        this.dtnSocket.handler(getSocketHandler(newState));
    }

    private Handler<Buffer> getSocketHandler(State state) {
        switch (state) {
            case READY: return buffer -> {
                ApiMessage response = ApiMessage.parse(buffer.toString());
                if (response.getCode() == StatusCode.NOTIFY_BUNDLE) {
                    LOGGER.debug("parsing bundle information: " + response.getMessage());
                    String bundleId = response.getMessage().split(" ")[0];

                    Function<NetSocket, Future<NetSocket>> loadBundleToQueue = apiCommand("bundle load queue", StatusCode.OK);
                    Function<NetSocket, Future<String>> getBundle = apiQuery("bundle get", StatusCode.OK);

                    loadBundleToQueue
                            .apply(dtnSocket)
                            .compose(getBundle)
                            .setHandler(loaded -> {
                                if (loaded.succeeded()) {
                                    LOGGER.debug(loaded.result());
                                }
                                else {
                                    LOGGER.warn("buuhh!");
                                }
                            });


                }
                else {
                    LOGGER.debug("NOT INTERESTED in {}", response.getCode().apiResponse());
                }
            };

            default: return buffer -> {
                LOGGER.debug("received {}", buffer);
            };
        }



    }

    private Function<NetSocket, Future<NetSocket>> apiCommand(String commandText, StatusCode expectedStatusCode) {
        return netSocket -> {
            Future<NetSocket> f = Future.future();
            netSocket.write(commandText + "\n").handler(buffer -> {
                ApiMessage response = ApiMessage.parse(buffer.toString());
                if (response.getCode() == expectedStatusCode) {
                    LOGGER.debug("{} completed", commandText);
                    f.complete(netSocket);
                } else {
                    LOGGER.warn("{} failed with unexpected response: {}", commandText, response);
                    f.fail("unexpected response: " + response);
                }
            });
            return f;
        };
    }

    private Function<NetSocket, Future<String>> apiQuery(String commandText, StatusCode expectedStatusCode) {
        StringBuilder bundle = new StringBuilder();

        return netSocket -> {
            Future<String> f = Future.future();
            netSocket.write(commandText + "\n").handler(buffer -> {
                bundle.append(buffer);
                if (buffer.toString().equals("\n")) {
                    f.complete(bundle.toString());
                }
            });
            return f;
        };
    }




}
