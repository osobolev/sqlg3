package sqlg3.runtime;

import java.sql.Connection;

public final class GContext {

    final GlobalContext global;
    private final Object userObject;
    final Connection connection;

    public GContext(GlobalContext global, Object userObject, Connection connection) {
        this.global = global;
        this.userObject = userObject;
        this.connection = connection;
    }

    public GlobalContext getGlobal() {
        return global;
    }

    public Object getUserObject() {
        return userObject;
    }
}
