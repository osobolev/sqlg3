package sqlg3.tx.runtime;

import java.sql.SQLException;

public final class SessionContext {

    final ConnectionManager cman;
    private final Object userObject;
    final PreCallCheck beforeCall;

    public SessionContext(ConnectionManager cman, Object userObject, PreCallCheck beforeCall) {
        this.cman = cman;
        this.userObject = userObject;
        this.beforeCall = beforeCall;
    }

    public void close() throws SQLException {
        cman.close();
    }

    public Object getUserObject() {
        return userObject;
    }
}
