package io.syncpoint.dtn.api;

public final class ApiMessage {

    private final StatusCode code;
    private final String message;


    // private constructor
    private ApiMessage(StatusCode code, String message) {
        this.code = code;
        this.message = message;
    }

    public static boolean isParseable(String candidate) {
        if (candidate == null || candidate.length() < 3) return false;
        try {
            final int responseCode = Integer.parseInt(candidate.substring(0, 3));
            if (StatusCode.codeOf(responseCode) != null) {
                return true;
            }
            return false;
        }
        catch (NumberFormatException e){
            return false;
        }
    }

    public static ApiMessage parse(String apiResponse) {
        StatusCode code = StatusCode.codeOf(Integer.parseInt(apiResponse.substring(0, 3)));

        String message = apiResponse.replace(code.apiResponse(), "").trim();
        return new ApiMessage(code, message);
    }


    @Override
    public String toString() {
        return code.toString() + " " + message;
    }
    public StatusCode getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }


}
