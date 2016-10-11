package io.syncpoint.dtn;

import io.syncpoint.dtn.api.ApiStatusResponse;
import io.syncpoint.dtn.api.StatusCode;
import io.syncpoint.dtn.connection.State;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.net.NetSocket;

import java.security.Provider;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public final class DtnBundleLoader extends AbstractConnectionVerticle {
    public DtnBundleLoader(){
        super();
        LOGGER = LoggerFactory.getLogger(DtnBundleLoader.class);
        //registerHandler();
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
                    f.fail(attempt.cause());
                }
            });


            return f;
        };

        Function<NetSocket, Future<NetSocket>> switchProtocol = netSocket -> {
            Future<NetSocket> f = Future.future();
            netSocket.write("protocol extended\n").handler(buffer -> {
                ApiStatusResponse response = ApiStatusResponse.parse(buffer.toString());
               if (response.getCode() == StatusCode.API_STATUS_OK) {
                   LOGGER.debug("switched to extended");
                   f.complete(netSocket);
               }
                else {
                   f.fail("unexpected response: " + response);
               }
            });

            return f;
        };

        Function<NetSocket, Future<NetSocket>> addRegistration = netSocket -> {
            Future<NetSocket> f = Future.future();
            netSocket.write("registration add dtn://tolerator/dem/dci\n").handler(buffer -> {
                ApiStatusResponse response = ApiStatusResponse.parse(buffer.toString());
                if (response.getCode() == StatusCode.API_STATUS_OK) {
                    LOGGER.debug("added registration");
                    f.complete(netSocket);
                }
                else {
                    f.fail("unexpected response: " + response);
                }
            });

            return f;
        };

        connectToApi.apply("172.16.125.133").compose(switchProtocol).compose(addRegistration).setHandler(h -> {
            if (h.succeeded()) {
                LOGGER.debug("becoming a listener ...");
            }
        });








    }



    private void registerHandler() {
        addSocketHandler(State.CONNECTED, buffer -> {
            become(State.SWITCHING);
            LOGGER.debug("sending protocol extended");
            dtnSocket.write("protocol extended\n");
        });

        addSocketHandler(State.SWITCHING, buffer -> {
            LOGGER.debug("verifying api response to " + buffer.toString());
            ApiStatusResponse response = ApiStatusResponse.parse(buffer.toString());
            if (StatusCode.API_STATUS_OK == response.getCode()) {
                become(State.READY);
                dtnSocket.write("noop\n");
            }
            else {
                LOGGER.warn("switching command failed: " + response);
            }
        });

        addSocketHandler(State.READY, buffer -> {
            LOGGER.debug("received " + buffer);
            become(State.RECEIVING);
            LOGGER.debug("sending registration");
            dtnSocket.write("registration add dtn://tolerator/dem/dci\n");
        });

        addSocketHandler(State.RECEIVING, buffer -> {
            LOGGER.debug("received " + buffer.toString());
            ApiStatusResponse response = ApiStatusResponse.parse(buffer.toString());
            if (StatusCode.API_STATUS_OK == response.getCode()) {
                LOGGER.info("last command succeeded: " + response);
            }
            else {
                LOGGER.warn("last command failed: " + response);
            }
        });


    }

}
