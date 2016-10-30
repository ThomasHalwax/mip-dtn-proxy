package io.syncpoint.dtn.bundle;

import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Base64;

public final class BlockAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(BlockAdapter.class);
    private final JsonObject block;

    public BlockAdapter() {
        this(new JsonObject());
    }

    public BlockAdapter(JsonObject block) {
        this.block = block;
    }

    public void setUnencodedContent(String content) {
        block.put(BundleFields.BLOCK_CONTENT_LENGTH, content.length());
        block.put(BundleFields.BLOCK_CONTENT, Base64.getEncoder().encodeToString(content.getBytes()));
        LOGGER.debug("content length is {}", content.length());
    }

    public void setEncodedContentLength(int length) {
        block.put(BundleFields.BLOCK_CONTENT_LENGTH, length);
    }

    public void setEncodedContent(String encodedContent) {
        block.put(BundleFields.BLOCK_CONTENT, encodedContent);
    }

    public String getEncodedContent() {
        return block.getString(BundleFields.BLOCK_CONTENT);
    }

    public void setFlag(BundleFlags flag, boolean value) {

    }

    public void setFlags(String flags) {
        final String[] values = flags.split(" ");
        for (String flag : values) {
            LOGGER.debug("SETTING FLAG {}", flag);
        }
    }

    public JsonObject getBlock() {
        return this.block.copy();
    }
}
