package io.syncpoint.dtn;

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
}
