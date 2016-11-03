package io.syncpoint.dtn;

public final class Helper {
    private Helper() {
        // utility class
    }

    public static String findElementValue(final String xmlElementName, final String source) {
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

}
