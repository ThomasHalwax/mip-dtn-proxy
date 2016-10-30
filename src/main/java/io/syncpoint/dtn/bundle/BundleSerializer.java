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
        BundleAdapter bundle = new BundleAdapter(rawBundle);

        Buffer buffer = Buffer.buffer();

        buffer.appendString("bundle put plain\n");
        buffer.appendString(BundleFields.SOURCE).appendString(": ")
                .appendString(bundle.getPrimaryBlockField(BundleFields.SOURCE)).appendString("\n");
        buffer.appendString(BundleFields.DESTINATION).appendString(": ")
                .appendString(bundle.getPrimaryBlockField(BundleFields.DESTINATION)).appendString("\n");
        buffer.appendString(BundleFields.BUNDLE_FLAGS).appendString(": ")
                .appendString(String.valueOf(bundle.getPrimaryBlockField(BundleFields.HEADER))).appendString("\n");
        buffer.appendString(BundleFields.NUMBER_OF_BLOCKS).appendString(": ")
                .appendString(String.valueOf(bundle.numberOfBlocksAdded())).appendString("\n\n");


        final Iterator<Object> blocks = bundle.blockIterator();
        while (blocks.hasNext()) {
            BlockAdapter block = new BlockAdapter((JsonObject)blocks.next());

            buffer.appendString(BundleFields.BLOCK_TYPE).appendString(": ")
                    .appendString(String.valueOf(block.getBlockType())).appendString("\n");
            buffer.appendString(BundleFields.BLOCK_FLAGS).appendString(": ")
                    .appendString(block.getFlags()).appendString("\n");
            buffer.appendString(BundleFields.BLOCK_CONTENT_LENGTH).appendString(": ")
                    .appendString(String.valueOf(block.getPlainContentLength())).appendString("\n\n");
            buffer.appendString(block.getEncodedContent()).appendString("\n\n");

        }
        buffer.appendString("bundle send\n\n");

        LOGGER.debug(buffer.toString());
        return buffer;
    }
}
