package io.syncpoint.dtn.api;

import java.util.HashMap;
import java.util.Map;

public enum StatusCode {
    API_STATUS_CONTINUE(100),
    API_STATUS_OK(200),
    API_STATUS_CREATED(201),
    API_STATUS_ACCEPTED(202),
    API_STATUS_FOUND(302),
    API_STATUS_BAD_REQUEST(400),
    API_STATUS_UNAUTHORIZED(401),
    API_STATUS_FORBIDDEN(403),
    API_STATUS_NOT_FOUND(404),
    API_STATUS_NOT_ALLOWED(405),
    API_STATUS_NOT_ACCEPTABLE(406),
    API_STATUS_CONFLICT(409),
    API_STATUS_INTERNAL_ERROR(500),
    API_STATUS_NOT_IMPLEMENTED(501),
    API_STATUS_SERVICE_UNAVAILABLE(503),
    API_STATUS_VERSION_NOT_SUPPORTED(505);

    private final Integer code;
    private static Map<Integer, StatusCode> map = new HashMap<>();

    private StatusCode(int code) {
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
        return code;
    }


}
