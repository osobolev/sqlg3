package sqlg3.runtime;

import sqlg3.core.IDBCommon;
import sqlg3.core.ISimpleTransaction;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.util.function.Consumer;

/**
 * This class can be used to call SQLG code from JDBC code. If you have {@link Connection} instance then
 * you can retrieve business interface <code>ITest</code> in the following way:
 * <pre>
 * Connection conn = ...;
 * ISimpleTransaction trans = new JdbcInterface(global, conn, false);
 * ITest iface = trans.getInterface(ITest.class);
 * </pre>
 */
public final class JdbcInterface implements ISimpleTransaction {

    private final TransactionContext transaction;
    private final boolean commitCalls;

    public JdbcInterface(GlobalContext global, Connection connection, boolean commitCalls, Object userObject, Consumer<Method> beforeCall) {
        SessionContext session = new SessionContext(new SingleConnectionManager(connection), userObject, beforeCall);
        this.transaction = new TransactionContext(global, session);
        this.commitCalls = commitCalls;
    }

    public <T extends IDBCommon> T getInterface(Class<T> iface) {
        return transaction.getInterface(iface, commitCalls, true);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private boolean commitCalls = false;
        private Object userObject = null;
        private Consumer<Method> beforeCall = null;

        public Builder setCommitCalls(boolean commitCalls) {
            this.commitCalls = commitCalls;
            return this;
        }

        public Builder setUserObject(Object userObject) {
            this.userObject = userObject;
            return this;
        }

        public Builder setBeforeCall(Consumer<Method> beforeCall) {
            this.beforeCall = beforeCall;
            return this;
        }

        public JdbcInterface build(GlobalContext global, Connection connection) {
            return new JdbcInterface(global, connection, commitCalls, userObject, beforeCall);
        }
    }
}
