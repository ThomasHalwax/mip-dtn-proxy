package io.syncpoint.dtn.bundle;

/**
 *
 * Block Flags: https://tools.ietf.org/html/rfc5050#page-15
 *
 *
 0
 6 5 4 3 2 1 0
 +-+-+-+-+-+-+-+
 |   Flags     |
 +-+-+-+-+-+-+-+

 Figure 4: Block Processing Control Flags Bit Layout

 0 - Block must be replicated in every fragment.

 1 - Transmit status report if block can't be processed.

 2 - Delete bundle if block can't be processed.

 3 - Last block.

 4 - Discard block if it can't be processed.

 5 - Block was forwarded without being processed.

 6 - Block contains an EID-reference field.
 *
 */


public enum BlockFlags {
    REPLICATE_IN_EVERY_FRAGMENT(0),
    TRANSMIT_STATUSREPORT_IF_NOT_PROCESSED(1),
    DELETE_BUNDLE_IF_NOT_PROCESSED(2),
    LAST_BLOCK(3),
    DISCARD_IF_NOT_PROCESSED(4),
    FORWARDED_WITHOUT_PROCESSED(5),
    BLOCK_CONTAINS_EIDS(6);

    private int offset;

    BlockFlags(int offset) {
        this.offset = offset;
    }

    public int getOffset() {
        return offset;
    }
}
