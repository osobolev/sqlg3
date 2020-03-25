package sqlg3.remote.common;

public enum HttpCommand {
    OPEN,
    GET_SESSIONS,
    GET_TRANSACTION,
    PING,
    CLOSE,
    KILL_SESSION,
    GET_CURRENT_SESSION,
    ROLLBACK,
    COMMIT,
    INVOKE,
    INVOKE_ASYNC
}
