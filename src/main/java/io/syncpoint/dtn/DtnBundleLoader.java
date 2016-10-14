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

                    Function<NetSocket, Future<NetSocket>> loadBundleToQueue = apiCommand("bundle load queue", StatusCode.OK);
                    Function<NetSocket, Future<NetSocket>> freeBundle = apiCommand("bundle free", StatusCode.OK);
                    Function<NetSocket, Future<Bundle>> getBundle = apiQuery("bundle get", StatusCode.OK);

                    loadBundleToQueue
                            .apply(dtnSocket)
                            .compose(getBundle)
                            .setHandler(loadedBundle -> {
                                if (loadedBundle.succeeded()) {
                                    freeBundle.apply(dtnSocket)
                                            .setHandler(allDone -> {
                                        if (allDone.succeeded()) {
                                            LOGGER.info("freed bundle");

                                        }
                                        else {
                                            LOGGER.warn("freed bundle failed!");
                                        }
                                    });
                                }
                                else {
                                    LOGGER.warn("failed to load bundle");
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
            netSocket.write(commandText + "\n").exceptionHandler(Future::failedFuture).handler(buffer -> {
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

    private Function<NetSocket, Future<Bundle>> apiQuery(String commandText, StatusCode expectedStatusCode) {

        return netSocket -> {
            QueryParser queryParser = new QueryParser(expectedStatusCode);
            //Future<Bundle> f = Future.future();
            netSocket.write(commandText + "\n").handler(buffer -> queryParser.handle(buffer));
            //return f;
            return queryParser.future();
        };
    }

    class QueryParser {

        private final StatusCode expectedStatusCode;
        private final Future<Bundle> parsedBundle = Future.future();
        private int lineNumber  = 0;

        private RecordParser recordParser = RecordParser.newDelimited("\n", buffer -> {
            handleInternal(buffer);
        });

        QueryParser(StatusCode expectedStatusCode) {
            this.expectedStatusCode = expectedStatusCode;
        }

        Future<Bundle> future() { return this.parsedBundle; }
        void handle(Buffer buffer) {
            recordParser.handle(buffer);
        }

        void handleInternal(Buffer buffer) {
            LOGGER.debug("handling line {} with content {}", lineNumber, buffer.toString());
            if (lineNumber == 0) {
                final ApiMessage apiMessage = ApiMessage.parse(buffer.toString());
                if (apiMessage.getCode() != expectedStatusCode) {
                    this.parsedBundle.fail("unexpected api response: " + apiMessage.toString());
                }
            }

            // TODO : create json object from received buffers
            if (lineNumber == 148) {
                LOGGER.debug("done :-) ");
                this.parsedBundle.complete(new Bundle());
            }

            lineNumber++;

        }

    }




}
