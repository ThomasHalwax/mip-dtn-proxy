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

    /**
     *
     * @param type A value of 1 marks this block as a payload (non-administrative) block.
     */
    public void setBlockType(int type) {
        block.put(BundleFields.BLOCK_TYPE, type);
    }

    public int getBlockType() {
        return block.getInteger(BundleFields.BLOCK_TYPE, 1);
    }

    /**
     *
     * @param content The content will be Base64 encoded and the original length will be
     *                be stored. Call {@link #getPlainContentLength()} to get this value.
     */
    public void setPlainContent(String content) {
        block.put(BundleFields.BLOCK_CONTENT_LENGTH, content.length());
        block.put(BundleFields.BLOCK_CONTENT, Base64.getEncoder().encodeToString(content.getBytes()));
        LOGGER.debug("content length is {}", content.length());
    }

    public int getPlainContentLength() {
        return block.getInteger(BundleFields.BLOCK_CONTENT_LENGTH, 0);
    }

    public void setEncodedContentLength(int length) {
        block.put(BundleFields.BLOCK_CONTENT_LENGTH, length);
    }

    public void setEncodedContent(String encodedContent) {
        block.put(BundleFields.BLOCK_CONTENT, encodedContent);
    }

    public String getEncodedContent() {
        return block.getString(BundleFields.BLOCK_CONTENT, "");
    }


    public void setFlag(BlockFlags flag, boolean value) {
        int currentFlags = 0;
        if (block.containsKey(BundleFields.BLOCK_FLAGS)) {
            currentFlags = block.getInteger(BundleFields.BLOCK_FLAGS);
        }

        BlockFlagsAdapter adapter = new BlockFlagsAdapter(currentFlags);
        adapter.set(flag, value);
        block.put(BundleFields.BLOCK_FLAGS, adapter.getFlags());
    }

    public void setFlags(String flags) {
        final String[] values = flags.split(" ");
        for (String flag : values) {
            try {
                LOGGER.debug("SETTING FLAG {}", flag);
                final BlockFlags parsedFlag = BlockFlags.valueOf(flag);
                setFlag(parsedFlag, true);
            }
            catch (IllegalArgumentException parsingFailed) {
                LOGGER.warn("failed to parse {} to a valid block flag: {}", flag, parsingFailed.getMessage());
            }
        }
    }

    public String getFlags() {
        int currentFlags = block.getInteger(BundleFields.BLOCK_FLAGS, 0);

        BlockFlagsAdapter adapter = new BlockFlagsAdapter(currentFlags);

        StringBuilder flags = new StringBuilder();

        for (BlockFlags flag : BlockFlags.values()) {
            if (adapter.get(flag)) {
                flags.append(flag.toString()).append(" ");
            }
        }
        return flags.toString().trim();
    }

    public JsonObject getBlock() {
        return this.block.copy();
    }
}
