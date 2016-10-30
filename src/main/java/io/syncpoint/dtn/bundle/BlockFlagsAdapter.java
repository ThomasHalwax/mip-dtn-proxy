package io.syncpoint.dtn.bundle;

public final class BlockFlagsAdapter {
    private int flags;

    public BlockFlagsAdapter(int flags) {
        this.flags = flags;
    }

    public BlockFlagsAdapter() {
        this(0);
    }

    public void set(BlockFlags flag, boolean value) {
        if (value) {
            flags |= 0b1 << flag.getOffset();
        } else {
            flags &= ~(0b1 << flag.getOffset());
        }
    }

    public Boolean get(BlockFlags flag) {
        int val = (0b1 << flag.getOffset());
        return (val & this.flags) == val;
    }

    public int getFlags() {
        return this.flags;
    }
}
