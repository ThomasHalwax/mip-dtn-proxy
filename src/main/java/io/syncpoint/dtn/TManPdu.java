package io.syncpoint.dtn;

import io.vertx.core.buffer.Buffer;

public final class TManPdu {
    private static final int TYPE_INDEX = 8;
    private final Buffer pdu;
    private final TManPduType pduType;

    public TManPdu(Buffer pdu) {
        final String msgType = pdu.getString(TYPE_INDEX, TYPE_INDEX + 2);
        switch (msgType) {
            case "T1": { pduType = TManPduType.T_OPNRQ; break;}
            case "T2": { pduType = TManPduType.T_DATA; break;}
            case "T3": { pduType = TManPduType.T_ABRT; break;}
            case "T4": { pduType = TManPduType.T_CHKCON; break;}
            default: { pduType = TManPduType.INVALID; break;}
        }
        this.pdu = pdu;
    }

    public Buffer getPdu() {
        return pdu;
    }

    public TManPduType getPduType() {
        return pduType;
    }
}
