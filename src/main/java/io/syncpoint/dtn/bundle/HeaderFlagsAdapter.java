package io.syncpoint.dtn.bundle;

public final class HeaderFlagsAdapter {
    private int flags;

    public HeaderFlagsAdapter(int flags) {
        this.flags = flags;
    }

    public void set(Flags flag, boolean value) {
        if (value) {
            flags |= 0b1 << flag.getOffset();
        } else {
            flags &= ~(0b1 << flag.getOffset());
        }
    }

    public Boolean get(Flags flag) {
        int val = (0b1 << flag.getOffset());
        return (val & this.flags) == val;
    }
}
