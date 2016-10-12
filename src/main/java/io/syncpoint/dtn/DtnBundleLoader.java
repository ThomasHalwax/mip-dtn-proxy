package io.syncpoint.dtn;

import io.syncpoint.dtn.api.ApiStatusResponse;
import io.syncpoint.dtn.api.StatusCode;
import io.syncpoint.dtn.connection.State;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetSocket;
import org.slf4j.Logger;

import java.util.function.Function;

public final class DtnBundleLoader extends AbstractVerticle {
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(DtnBundleLoader.class);
    private NetSocket dtnSocket;
    private State state = State.DISCONNECTED;

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

        Function<NetSocket, Future<NetSocket>> switchProtocol = apiCommand("protocol extended", StatusCode.API_STATUS_OK);
        Function<NetSocket, Future<NetSocket>> addRegistration = apiCommand("registration add dtn://tolerator/dem/dci", StatusCode.API_STATUS_OK);

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
        LOGGER.debug("transition from {} to {}", this.state, newState);
        this.state = newState;
        this.dtnSocket.handler(getSocketHandler(newState));
    }

    private Handler<Buffer> getSocketHandler(State newState) {
        return buffer -> {
            LOGGER.debug("received {}", buffer);
        };
    }

    private Function<NetSocket, Future<NetSocket>> apiCommand(String commandText, StatusCode expectedStatusCode) {
        return netSocket -> {
            Future<NetSocket> f = Future.future();
            netSocket.write(commandText + "\n").handler(buffer -> {
                ApiStatusResponse response = ApiStatusResponse.parse(buffer.toString());
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


}
