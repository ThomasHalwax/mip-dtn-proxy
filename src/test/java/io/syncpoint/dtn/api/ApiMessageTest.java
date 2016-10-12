package io.syncpoint.dtn.api;

import org.junit.Assert;
import org.junit.Test;

public final class ApiMessageTest {

    @Test
    public void bundleNotification() {
        final String response = "602 NOTIFY BUNDLE 529609580 1 dtn://node2.dtn/UYMMFcBHOAvLWuZF";
        ApiMessage apiMessage = ApiMessage.parse(response);

        Assert.assertEquals(StatusCode.NOTIFY_BUNDLE, apiMessage.getCode());
        Assert.assertEquals("602 NOTIFY BUNDLE", apiMessage.getCode().apiResponse());
        Assert.assertEquals("529609580 1 dtn://node2.dtn/UYMMFcBHOAvLWuZF", apiMessage.getMessage());
    }
}
