package io.syncpoint.dtn.api;

public final class ApiMessage {

    private final StatusCode code;
    private final String message;


    // private constructor
    private ApiMessage(StatusCode code, String message) {
        this.code = code;
        this.message = message;
    }

    public static ApiMessage parse(String apiResponse) {
        StatusCode code = StatusCode.codeOf(Integer.parseInt(apiResponse.substring(0, 3)));

        String message = apiResponse.replace(code.apiResponse(), "").trim();
        return new ApiMessage(code, message);
    }

    public StatusCode getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return code.toString() + " " + message;
    }
}
