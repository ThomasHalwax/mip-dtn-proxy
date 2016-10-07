package io.syncpoint.dtn.api;

public final class ApiStatusResponse {

    private final StatusCode code;
    private final String message;

    // private constructor
    private ApiStatusResponse(StatusCode code, String message) {
        this.code = code;
        this.message = message;
    }

    public static ApiStatusResponse parse(String apiResponse) {
        StatusCode code = StatusCode.codeOf(Integer.parseInt(apiResponse.substring(0, 3)));
        String message = apiResponse.substring(4);
        return new ApiStatusResponse(code, message);
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
