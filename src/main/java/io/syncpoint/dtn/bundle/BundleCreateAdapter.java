package io.syncpoint.dtn.bundle;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

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

    public void setHeaderFlags(Flags flag, boolean value) {
        flagsAdapter.set(flag, value);
        header.put(BundleFields.FLAGS, flagsAdapter.getFlags());
    }

    public void addBase64Payload(String encodedPayload) {
        JsonObject block = new JsonObject();
        block.put(BundleFields.PAYLOAD, encodedPayload);
        blocks.add(block);
    }
}
