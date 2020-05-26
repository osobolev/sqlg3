package sqlg3.runtime;

import java.sql.SQLException;

public final class SessionContext {

    final ConnectionManager cman;
    private final Object userObject;

    public SessionContext(ConnectionManager cman, Object userObject) {
        this.cman = cman;
        this.userObject = userObject;
    }

    public void close() throws SQLException {
        cman.close();
    }

    public Object getUserObject() {
        return userObject;
    }
}
