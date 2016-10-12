package io.syncpoint.dtn.api;

import org.junit.Assert;
import org.junit.Test;

public final class StatusCodeTest {

    @Test
    public void statusCodeAsString() {
        int code = 602;
        final StatusCode statusCode = StatusCode.codeOf(code);
        Assert.assertEquals("602 NOTIFY BUNDLE", statusCode.apiResponse());
    }
}
