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
            if (uri.isAbsolute()) {
                registry.put(channel, uri.getHost());
                LOGGER.debug("added channel {} will resolve to host {}", channel, uri.getHost());
            }
            else {
                registry.put(channel, host);
                LOGGER.debug("added channel {} will resolve to host {}", channel, host);
            }

        } catch (URISyntaxException e) {
            LOGGER.warn("{} is not a valid host");
        }
    }

    public void unregisterChannel(String channel) {
        final String removed = registry.remove(channel);
        if (removed == null) {
            LOGGER.warn("cannot unregister non-existing channel: {}", channel);
        }
    }

    public String getHostForChannel(String channel) {

        String host = registry.get(channel);
        if (host != null) {
            LOGGER.debug("returning host {} for channel {}", host, channel);
            return host;
        }

        final String[] pathElements = channel.split("/");
        String accumulator = "";

        for (int i=0; i < pathElements.length; i++) {
            accumulator = accumulator + "/" + pathElements[i];
            if (accumulator.equals("/")) {
                accumulator = "";
                continue;}
            if (registry.containsKey(accumulator)) {
                LOGGER.debug("found host for shortest path component {}", accumulator);
                return registry.get(accumulator);
            }
            else {
                LOGGER.debug("nothing found for accumulated channel {}", accumulator);
            }
        }
        return null;
    }

    public boolean hasHostForChannel(String channel) {
        LOGGER.debug("resolving host for channel {}", channel);
        return registry.containsKey(channel);
    }
}
