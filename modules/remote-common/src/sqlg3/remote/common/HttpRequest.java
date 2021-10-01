package sqlg3.remote.common;

import sqlg3.core.IDBCommon;

import java.io.Serializable;

public final class HttpRequest implements Serializable {

    private static final HttpCommand[] COMMANDS = HttpCommand.values();

    public final HttpId id;
    public final byte commandIndex;
    public final Class<? extends IDBCommon> iface;
    public final String method;
    public final Class<?>[] paramTypes;
    public final Object[] params;

    public HttpRequest(HttpId id, HttpCommand command, Class<? extends IDBCommon> iface, String method, Class<?>[] paramTypes, Object[] params) {
        this.id = id;
        this.commandIndex = (byte) command.ordinal();
        this.iface = iface;
        this.method = method;
        this.paramTypes = paramTypes;
        this.params = params;
    }

    public HttpCommand getCommand() {
        return COMMANDS[commandIndex];
    }
}
