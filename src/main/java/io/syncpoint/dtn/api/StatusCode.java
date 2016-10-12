package io.syncpoint.dtn.api;

import java.util.HashMap;
import java.util.Map;

public enum StatusCode {
    CONTINUE(100),
    OK(200),
    CREATED(201),
    ACCEPTED(202),
    FOUND(302),
    UNKNOWN_COMMAND(400),
    UNAUTHORIZED(401),
    FORBIDDEN(403),
    NOT_FOUND(404),
    ALLOWED(405),
    NOT_ACCEPTABLE(406),
    CONFLICT(409),
    INTERNAL_ERROR(500),
    NOT_IMPLEMENTED(501),
    SERVICE_UNAVAILABLE(503),
    VERSION_NOT_SUPPORTED(505),
    NOTIFY_BUNDLE(602);

    private final Integer code;
    private static Map<Integer, StatusCode> map = new HashMap<>();

    StatusCode(int code) {
        this.code = code;
    }

    static {
        for (StatusCode statusCode : StatusCode.values()) {
            map.put(statusCode.code, statusCode);
        }
    }

    public static StatusCode codeOf(Integer code) {
        return map.get(code);
    }

    public int getCode() {
        return this.code;
    }


    public String apiResponse() {
        return this.code + " " + map.get(this.code).toString().replaceAll("_", " ");
    }


}
