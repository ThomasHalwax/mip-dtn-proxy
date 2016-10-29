package io.syncpoint.dtn.bundle;

import io.netty.buffer.ByteBuf;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.Base64;

public final class BundleCreateAdapter {
    private final JsonObject bundle = new JsonObject();
    private final JsonObject header = new JsonObject();
    private final JsonArray blocks = new JsonArray();
    private final HeaderFlagsAdapter flagsAdapter = new HeaderFlagsAdapter();

    public BundleCreateAdapter() {
        bundle.put(BundleFields.HEADER, header);
        bundle.put(BundleFields.BLOCKS, blocks);
    }

    public void setSource(String source) {
        header.put(BundleFields.SOURCE, source);
    }

    public void setDestination(String destination) {
        header.put(BundleFields.DESTINATION, destination);
    }

    public void setHeaderFlags(BundleFlags flag, boolean value) {
        flagsAdapter.set(flag, value);
        header.put(BundleFields.BUNDLE_FLAGS, flagsAdapter.getFlags());
    }

    public void addPayload(String payload) {
        JsonObject block = new JsonObject();
        block.put(BundleFields.BLOCK_LENGTH, payload.length());
        block.put(BundleFields.PAYLOAD, Base64.getEncoder().encodeToString(payload.getBytes()));
        blocks.add(block);
    }

    public String encode() {
        return bundle.encode();
    }

    public JsonObject getCopy() {
        return bundle.copy();
    }

}
