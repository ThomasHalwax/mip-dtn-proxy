package io.syncpoint.dtn;

import java.net.URI;
import java.net.URISyntaxException;

public final class Helper {
    private Helper() {
        // utility class
    }

    public static String getElementValue(final String xmlElementName, final String source) {
        final String startElement = "<" + xmlElementName + ">";
        final String endElement = "</" + xmlElementName + ">";

        final int startPos = source.indexOf(startElement);
        if (startPos == -1) {
            return "";
        }

        final int endPos = source.indexOf(endElement, startPos);
        if (endPos == -1) {
            return "";
        }

        return source.substring(startPos + startElement.length(), endPos);
    }

    /**
     *
     * @param tOpenRequest
     * @return the destination node ID
     */
    public static String getDestinationNodeId(String tOpenRequest) {
        // 00000022T1|123456789|987654321
        return tOpenRequest.substring(21, tOpenRequest.length());
    }

    public static String getSourceNodeId(String tOpenRequest) {
        // 00000022T1|123456789|987654321
        return tOpenRequest.substring(11, 20);
    }

    public static String getDtnHostFromUri(String address) {
        try {
            URI url = new URI(address);
            return url.getScheme() + "://" + url.getHost();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public static String getChannelFromUri(String address) {
        try {
            URI url = new URI(address);
            //String channel = url.getPath().replace(prefix, "");
            //if (channel.startsWith("/")) {
            //    channel = channel.substring(1, channel.length());
            //}
            return url.getPath();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }


}
