package io.syncpoint.dtn;

import org.junit.Assert;
import org.junit.Test;

public class ResolverTest {

    @Test
    public void resolveWithGrowingPath() {
        String channelPath = "/dem/999111999/123123123/701484f3-f1ca-4d2f-8512-ef1974f5b9d9";
        String demPath = "/dem/999111999";
        String host = "some.host";

        Resolver resolver = new Resolver();
        resolver.registerHostForChannel(demPath, host);

        final String hostForChannel = resolver.getHostForChannel(channelPath);
        Assert.assertEquals(host, hostForChannel);
    }

    @Test
    public void resolveHost$withSchema() {
        String demPath = "/dem/999111999";
        String host = "some.host";

        Resolver resolver = new Resolver();
        resolver.registerHostForChannel(demPath, "dtn://" + host);

        final String hostForChannel = resolver.getHostForChannel(demPath);
        Assert.assertEquals(host, hostForChannel);
    }

    @Test
    public void resolveHost$withoutSchema() {
        String demPath = "/dem/999111999";
        String host = "some.host";

        Resolver resolver = new Resolver();
        resolver.registerHostForChannel(demPath, host);

        final String hostForChannel = resolver.getHostForChannel(demPath);
        Assert.assertEquals(host, hostForChannel);
    }
}
