package io.syncpoint.dtn;

public final class Addresses {
    private Addresses() {
        // utility class
    }

    public static final String PREFIX = "dem://";

    // DTN related addresses
    public static final String DTN_DCI_ANNOUNCE_ADDRESS = PREFIX + "dci/announce";
    public static final String DTN_DCI_REPLY_ADDRESS = PREFIX + "dci/reply";
    public static final String DTN_REPORT_TO_ADDRESS = "dem/report";



    // vertx eventbus related addresses
    public static final String COMMAND_ANNOUNCE_DCI = "command://dci/announce";
    public static final String EVENT_DCI_ANNOUNCED = "event://dci/announced";

    public static final String COMMAND_REPLY_DCI = "command://dci/reply";
    public static final String EVENT_DCI_REPLIED = "event://dci/replied";

    public static final String EVENT_BUNDLE_RECEIVED = "event://bundle/received";
    public static final String COMMAND_SEND_BUNDLE = "command://bundle/send";

    public static final String COMMAND_REGISTER_PROXY = "command://proxy/register";
    public static final String COMMAND_UNREGISTER_PROXY = "command://proxy/unregister";

    public static final String COMMAND_SEND_TMAN_PDU = "command://tman/send";

    public static final String COMMAND_SEND_CLOSE_SOCKET = "command://socket/close";
    public static final String EVENT_SOCKET_CLOSED = "event://socket/closed";

    public static final String COMMAND_ADD_REGISTRATION = "command://registration/add";
    public static final String COMMAND_DELETE_REGISTRATION = "command://registration/del";

    public static final String QUERY_NODENAME = "query://nodename";

}
