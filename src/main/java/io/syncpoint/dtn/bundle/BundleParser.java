package io.syncpoint.dtn.bundle;

import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BundleParser {
    private static final Logger LOGGER = LoggerFactory.getLogger(BundleParser.class);

    BAdapter bundleAdapter = new BAdapter();
    BlockAdapter blockAdapter = new BlockAdapter();

    private StringBuilder blockContent = new StringBuilder();
    private BundleSection bundleSection = BundleSection.PRIMARY_BLOCK;

    public void reset() {
        bundleAdapter = new BAdapter();
        blockAdapter = new BlockAdapter();
        blockContent = new StringBuilder();
        bundleSection = BundleSection.PRIMARY_BLOCK;
    }

    public void addData(String data) {

        switch (bundleSection) {
            case PRIMARY_BLOCK: {
                if (data.length() == 0) {
                    bundleSection = BundleSection.BLOCK_HEADER;
                    LOGGER.debug("reached end of header");
                }
                else {
                    final String[] headerLine = data.split(": ");
                    if (headerLine.length != 2) return;
                    bundleAdapter.setPrimaryBlockField(headerLine[0], headerLine[1]);
                }
                break;
            }
            case BLOCK_HEADER: {
                if (data.length() == 0) {
                    bundleSection = BundleSection.BLOCK_CONTENT;
                    LOGGER.debug("reached end of block header");
                }
                else {
                    final String[] blockHeaderLine = data.split(": ");
                    if (blockHeaderLine.length != 2) return;
                    switch (blockHeaderLine[0]) {
                        case BundleFields.BLOCK_FLAGS: {
                            blockAdapter.setFlags(blockHeaderLine[1]);
                            break;
                        }
                        case BundleFields.BLOCK_CONTENT_LENGTH: {
                            blockAdapter.setEncodedContentLength(Integer.parseInt(blockHeaderLine[1]));
                            break;
                        }
                        default: {
                            LOGGER.debug("ignoring block data {}", data);
                        }
                    }
                }
                break;
            }
            case BLOCK_CONTENT: {
                if (data.length() == 0) {
                    blockAdapter.setEncodedContent(blockContent.toString());
                    bundleAdapter.addBlock(blockAdapter.getBlock());

                    if (moreBlocks()) {
                        blockAdapter = new BlockAdapter();
                        blockContent = new StringBuilder();
                        return;
                    }
                    else {
                        LOGGER.info("end of bundle reached");
                        bundleSection = BundleSection.END;
                    }
                    return;
                }
                else {
                    blockContent.append(data);
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

    public JsonObject getBundle() {
        return this.bundleAdapter.getBundle();
    }

    private boolean moreBlocks() {
        if (bundleAdapter.numberOfBlocksAdded() == bundleAdapter.numberOfBlocksExpected()) {
            return false;
        }
        return true;
    }
}
