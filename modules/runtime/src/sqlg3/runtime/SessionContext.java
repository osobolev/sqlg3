package sqlg3.runtime;

import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.function.Consumer;

public final class SessionContext {

    final ConnectionManager cman;
    private final Object userObject;
    final Consumer<Method> beforeCall;

    public SessionContext(ConnectionManager cman, Object userObject, Consumer<Method> beforeCall) {
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
