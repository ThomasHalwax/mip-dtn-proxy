package io.syncpoint.dtn.connection;

public enum State {
    DISCONNECTED,
    CONNECTED,
    SWITCHING,
    READY,
    AWAIT_RESPONSE
}
