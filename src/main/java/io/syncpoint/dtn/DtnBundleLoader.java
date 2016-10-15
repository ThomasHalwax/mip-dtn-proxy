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

        // register self handler
        vertx.eventBus().consumer(self(), messageToSelf -> {
            LOGGER.debug("received a message to self: {}", messageToSelf.toString());
        });


        Function<String, Future<Void>> connectToApi = host -> {
            Future<Void> f = Future.future();

            vertx.createNetClient().connect(4550, host, attempt -> {
                if (attempt.succeeded()) {
                    LOGGER.debug("connected");
                    dtnSocket = attempt.result();
                    dtnSocket.handler(buffer -> {
                       LOGGER.debug("onConnect: " + buffer.toString());
                        f.complete();
                    });
                    dtnSocket.write("\n");
                }
                else {
                    LOGGER.warn("connect failed ", attempt.cause());
                    f.fail(attempt.cause());
                }
            });
            return f;
        };

        Function<Void, Future<Void>> switchProtocol = apiCommand("protocol extended", StatusCode.OK);
        Function<Void, Future<Void>> addRegistration = apiCommand("registration add dtn://tolerator/dem/dci", StatusCode.OK);

        // TODO: when there are persisted bundles the daemon
        // will send their ID after registration
        // idea is to publish the id on the eventbus to self()
        // and process them later
        connectToApi
                .apply("172.16.125.133")
                .compose(switchProtocol)
                .compose(addRegistration)
                .setHandler(h -> {
                    LOGGER.debug("READY to interact with the IBRDTN API");
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

                    Function<Void, Future<Void>> loadBundleToQueue = apiCommand("bundle load queue", StatusCode.OK);
                    Function<Void, Future<Void>> freeBundle = apiCommand("bundle free", StatusCode.OK);
                    Function<Void, Future<Bundle>> getBundle = apiQuery("bundle get", StatusCode.OK);


                    loadBundleToQueue.apply(null)
                            .compose(getBundle).setHandler(bundle -> {
                                if (bundle.succeeded()) {
                                    freeBundle.apply(null).setHandler(allDone -> {
                                        LOGGER.debug("all done");
                                    });
                                }
                                else {
                                    LOGGER.warn("failed to get the bundle");
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

    private Function<Void, Future<Void>> apiCommand(String commandText, StatusCode expectedStatusCode) {
        return empty -> {
            Future<Void> f = Future.future();
            dtnSocket.write(commandText + "\n").exceptionHandler(Future::failedFuture).handler(buffer -> {
                ApiMessage response = ApiMessage.parse(buffer.toString());
                if (response.getCode() == expectedStatusCode) {
                    LOGGER.debug("{} completed", commandText);
                    LOGGER.debug("response was {}", response.getMessage());
                    become(State.READY);
                    f.complete();
                } else {
                    LOGGER.warn("{} failed with unexpected response: {}", commandText, response);
                    f.fail("unexpected response: " + response);
                }
            });
            return f;
        };
    }

    private Function<Void, Future<Bundle>> apiQuery(String commandText, StatusCode expectedStatusCode) {

        return empty -> {
            QueryParser queryParser = new QueryParser(expectedStatusCode);
            //Future<Bundle> f = Future.future();
            dtnSocket.write(commandText + "\n").handler(buffer -> queryParser.handle(buffer));
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
                return;
            }

            lineNumber++;

        }
    }


    private String self() {
        return deploymentID();
    }
}
