package io.syncpoint.dtn;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

public final class Resolver {
    private static final Logger LOGGER = LoggerFactory.getLogger(Resolver.class);

    private Map<String, String> registry = new HashMap<>();

    public void registerHostForChannel(String channel, String host) {
        try {
            URI uri = new URI(host);
            registry.put(channel, uri.getHost());
            LOGGER.debug("added channel {} will resolve to host {}", channel, uri.getHost());
        } catch (URISyntaxException e) {
            LOGGER.warn("{} is not a valid host");
        }
    }

    public String getHostForChannel(String channel) {
        LOGGER.debug("returning host {} for channel {}", registry.get(channel), channel);
        return registry.get(channel);
    }

    public boolean hasHostForChannel(String channel) {
        LOGGER.debug("resolving host for channel {}", channel);
        return registry.containsKey(channel);
    }
}
