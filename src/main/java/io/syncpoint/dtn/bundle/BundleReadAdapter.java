package io.syncpoint.dtn.bundle;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.Iterator;

public final class BundleReadAdapter {
    private final JsonObject bundle;
    private final JsonObject header;
    private final JsonArray blocks;
    private HeaderFlagsAdapter headerFlagsAdapter;

    public BundleReadAdapter(JsonObject bundle) {
        this.bundle = bundle;
        header = bundle.getJsonObject("header");
        String flags = header.getString(BundleFields.BUNDLE_FLAGS);
        headerFlagsAdapter = new HeaderFlagsAdapter(Integer.parseInt(flags));
        blocks = bundle.getJsonArray("blocks");
    }

    public String destination() {
        return header.getString("Destination");
    }

    public String source() {
        return header.getString("Source");
    }

    public int numberOfBlocks() {
        return blocks.size();
    }

    public Iterator<Object> blocks() {
        return this.blocks.iterator();
    }

    public boolean getHeaderFlag(BundleFlags flag) {
        return headerFlagsAdapter.get(flag);
    }

    public int headerFlags() { return header.getInteger(BundleFields.BUNDLE_FLAGS); }
}
