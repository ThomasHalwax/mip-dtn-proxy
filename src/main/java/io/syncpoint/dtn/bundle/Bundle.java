package io.syncpoint.dtn.bundle;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Bundle extends JsonObject {
    private static final Logger LOGGER = LoggerFactory.getLogger(Bundle.class);


    private final JsonArray blocks = new JsonArray();
    private final JsonObject header = new JsonObject();
    private final StringBuilder currentPayload = new StringBuilder();

    private JsonObject currentBlock = new JsonObject();
    private BundleSection bundleSection = BundleSection.HEADER;
    private Integer blocksAdded = 0;

    public Bundle() {
        this.put("header", header);
        this.put("blocks", blocks);
    }

    public void addData(String data) {
        //LOGGER.debug("<< {}", data);
        switch (bundleSection) {
            case HEADER: {
                if (data.length() == 0) {
                    bundleSection = BundleSection.BLOCK_HEADER;
                    LOGGER.debug("reached end of header");
                }
                else {
                    final String[] headerLine = data.split(": ");
                    if (headerLine.length != 2) {
                        LOGGER.warn("splitting failed for {} with length {}", data, data.length());
                    }
                    header.put(headerLine[0], headerLine[1]);
                }
                break;
            }
            case BLOCK_HEADER: {
                if (data.length() == 0) {
                    bundleSection = BundleSection.BLOCK_PAYLOAD;
                    LOGGER.debug("reached end of block header");
                }
                else {
                    final String[] blockHeaderLine = data.split(": ");
                    currentBlock.put(blockHeaderLine[0], blockHeaderLine[1]);
                }
                break;
            }
            case BLOCK_PAYLOAD: {
                if (data.length() == 0) {
                    currentBlock.put("payload", currentPayload.toString());
                    blocks.add(currentBlock);
                    blocksAdded++;
                    if (moreBlocks()) {
                        currentBlock = new JsonObject();
                        blocksAdded = 0;
                        currentBlock.clear();
                        return;
                    }
                    else {
                        LOGGER.info("end of bundle reached");
                        bundleSection = BundleSection.END;
                    }
                    return;
                }
                else {
                    currentPayload.append(data);
                }
                break;
            }
            case END: {
                LOGGER.error("invalid operation! End of bundle is already reached.");
                break;
            }
        }
    }

    public boolean done() {
        return (bundleSection == BundleSection.END);
    }


    private boolean moreBlocks() {
        if (header.getString("Blocks").equals(blocksAdded.toString())) {
            return false;
        }
        return true;
    }
}
