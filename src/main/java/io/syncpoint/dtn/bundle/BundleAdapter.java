package io.syncpoint.dtn.bundle;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.Iterator;

public final class BundleAdapter {

    private final JsonObject bundle;
    private final JsonArray blocks;

    public BundleAdapter() {
        this(new JsonObject());
    }

    public BundleAdapter(JsonObject bundle) {
        this.bundle = bundle;
        if (bundle.containsKey(BundleFields.BLOCKS)) {
            blocks = bundle.getJsonArray(BundleFields.BLOCKS);
        }
        else {
            blocks = new JsonArray();
            bundle.put(BundleFields.BLOCKS, blocks);
        }
    }

    public void setPrimaryBlockField(String key, String value) {
        bundle.put(key, value);
    }

    public String getPrimaryBlockField(String key) {
        String value = bundle.getString(key, "");
        if (value == null) {
            return "";
        }
        return value;
    }

    public boolean hasPrimaryBlockField(String key) {
        return bundle.containsKey(key);
    }

    public void setSource(String source) {
        setPrimaryBlockField(BundleFields.SOURCE, source);
    }

    public String getSource() {
        return getPrimaryBlockField(BundleFields.SOURCE);
    }

    public void setDestination(String source) {
        setPrimaryBlockField(BundleFields.DESTINATION, source);
    }

    public String getDestination() {
        return getPrimaryBlockField(BundleFields.DESTINATION);
    }

    public void addBlock(JsonObject block) {

        blocks.iterator().forEachRemaining(b -> {
            BlockAdapter iteratingAdapter = new BlockAdapter((JsonObject)b);
            iteratingAdapter.setFlag(BlockFlags.LAST_BLOCK, false);
        });

        BlockAdapter adapter = new BlockAdapter(block);
        adapter.setFlag(BlockFlags.LAST_BLOCK, true);
        blocks.add(block);
    }

    public int numberOfBlocksAdded() {
        return blocks.size();
    }
    public int numberOfBlocksExpected() { return Integer.parseInt(bundle.getString(BundleFields.NUMBER_OF_BLOCKS, "0"));}

    public Iterator<Object> blockIterator() {
        return blocks.iterator();
    }

    public JsonObject getBundle() {
        return this.bundle.copy();
    }

}
