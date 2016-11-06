package io.syncpoint.dtn.api;

import java.util.function.Consumer;

public final class ApiResponse implements Consumer<ApiMessage> {
    private final StatusCode expectedStatusCode;
    private Consumer<ApiMessage> successHandler = defaultSuccessHandler();
    private Consumer<ApiMessage> failHandler = defaultFailHandler();

    public ApiResponse(StatusCode expectedStatusCode) {
        this.expectedStatusCode = expectedStatusCode;
    }

    @Override
    public void accept(ApiMessage apiMessage) {
        if (apiMessage.getCode() == this.expectedStatusCode) {
            successHandler.accept(apiMessage);
        }
        else {
            failHandler.accept(apiMessage);
        }
    }

    public void successHandler(Consumer<ApiMessage> handler) {
        this.successHandler = handler;
    }

    public void failHandler(Consumer<ApiMessage> handler) {
        this.failHandler = handler;
    }

    private Consumer<ApiMessage> defaultSuccessHandler() {
        return apiMessage -> {};
    }

    private Consumer<ApiMessage> defaultFailHandler() {
        return apiMessage -> {};
    }

}
