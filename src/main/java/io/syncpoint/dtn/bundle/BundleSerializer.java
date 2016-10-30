package io.syncpoint.dtn.bundle;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;

public final class BundleSerializer {
    private static final Logger LOGGER = LoggerFactory.getLogger(BundleSerializer.class);
    private BundleSerializer() {
        // utility class
    }

    public static Buffer serialize(JsonObject rawBundle) {
        BAdapter bundle = new BAdapter(rawBundle);

        Buffer buffer = Buffer.buffer();
        //buffer.appendString("bundle clear\n");
        buffer.appendString("bundle put plain\n");
        buffer.appendString(BundleFields.SOURCE).appendString(": ")
                .appendString(bundle.getPrimaryBlockField(BundleFields.SOURCE)).appendString("\n");
        buffer.appendString(BundleFields.DESTINATION).appendString(": ")
                .appendString(bundle.getPrimaryBlockField(BundleFields.DESTINATION)).appendString("\n");
        buffer.appendString(BundleFields.BUNDLE_FLAGS).appendString(": ")
                .appendString(String.valueOf(bundle.getPrimaryBlockField(BundleFields.HEADER))).appendString("\n");
        buffer.appendString(BundleFields.NUMBER_OF_BLOCKS).appendString(": ")
                .appendString(String.valueOf(bundle.numberOfBlocksAdded())).appendString("\n\n");

        int blockId = 1;
        final Iterator<Object> blocks = bundle.blockIterator();
        while (blocks.hasNext()) {
            JsonObject block = (JsonObject)blocks.next();
            String base64EncodedPayload = block.getString(BundleFields.BLOCK_CONTENT);
            LOGGER.debug("blocksize is {}", base64EncodedPayload.length());
            buffer.appendString(BundleFields.CURRENT_BLOCK).appendString(": ").appendString(String.valueOf(blockId)).appendString("\n");
            buffer.appendString(BundleFields.BLOCK_FLAGS).appendString(": ").appendString("LAST_BLOCK").appendString("\n");
            buffer.appendString(BundleFields.BLOCK_CONTENT_LENGTH).appendString(": ").appendString(String.valueOf(block.getInteger(BundleFields.BLOCK_CONTENT_LENGTH))).appendString("\n\n");
            buffer.appendString(base64EncodedPayload).appendString("\n\n");
            blockId++;
        }
        buffer.appendString("bundle send\n\n");

        LOGGER.debug(buffer.toString());
        return buffer;
    }
}
