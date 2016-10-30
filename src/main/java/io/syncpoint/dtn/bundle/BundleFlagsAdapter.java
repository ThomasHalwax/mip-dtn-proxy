package io.syncpoint.dtn.bundle;

public final class BundleFlagsAdapter {
    private int flags;

    public BundleFlagsAdapter(int flags) {
        this.flags = flags;
    }

    public BundleFlagsAdapter() {
        this(0);
    }

    public void set(BundleFlags flag, boolean value) {
        if (value) {
            flags |= 0b1 << flag.getOffset();
        } else {
            flags &= ~(0b1 << flag.getOffset());
        }
    }

    public Boolean get(BundleFlags flag) {
        int val = (0b1 << flag.getOffset());
        return (val & this.flags) == val;
    }

    public int getFlags() {
        return this.flags;
    }
}
