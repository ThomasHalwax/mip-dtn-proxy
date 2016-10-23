package io.syncpoint.dtn.bundle;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.Iterator;

public final class BundleAdapter {
    private final JsonObject bundle;
    private final JsonObject header;
    private final JsonArray blocks;
    private HeaderFlagsAdapter headerFlagsAdapter;

    public BundleAdapter(JsonObject bundle) {
        this.bundle = bundle;
        header = bundle.getJsonObject("header");
        headerFlagsAdapter = new HeaderFlagsAdapter(Integer.parseInt(header.getString("Processing flags")));
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

    public boolean getHeaderFlag(Flags flag) {
        return headerFlagsAdapter.get(flag);
    }
}
