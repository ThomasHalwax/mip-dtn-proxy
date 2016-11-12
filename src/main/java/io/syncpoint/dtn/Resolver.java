package io.syncpoint.dtn;

import java.util.HashMap;
import java.util.Map;

public final class Resolver {

    private Map<String, String> registry = new HashMap<>();

    public void registerHostForChannel(String channel, String host) {

        registry.put(channel, host);
    }
    public String getHostForChannel(String channel) {
        return registry.get(channel);
    }
    public boolean hasHostForChannel(String channel) {

        return registry.containsKey(channel);
    }

}
