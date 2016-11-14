package io.syncpoint.dtn;

public final class Addresses {
    private Addresses() {
        // utility class
    }

    public static final String DTN_PREFIX = "dtn://";
    public static final String DTN_BROADCAST_HOST = "broadcast.any";
    public static final String APP_PREFIX = "dem";

    // DTN related addresses
    public static final String DTN_DCI_ANNOUNCE_ADDRESS = DTN_PREFIX + APP_PREFIX + "/dci/announce";
    public static final String DTN_DCI_REPLY_ADDRESS = DTN_PREFIX + APP_PREFIX + "/dci/reply";
    public static final String DTN_REPORT_TO_ADDRESS = APP_PREFIX + "/report";

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

    public static final String COMMAND_ADD_ENDPOINT = "command://endpoint/add";
    public static final String COMMAND_DELETE_ENDPOINT = "command://endpoint/del";

    public static final String QUERY_NODENAME = "query://nodename";

}
