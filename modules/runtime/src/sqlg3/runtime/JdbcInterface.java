package sqlg3.runtime;

import sqlg3.core.IDBCommon;
import sqlg3.core.ISimpleTransaction;

import java.sql.Connection;

/**
 * This class can be used to call SQLG code from JDBC code. If you have {@link Connection} instance then
 * you can retrieve business interface <code>ITest</code> in the following way:
 * <pre>
 * Connection conn = ...;
 * ISimpleTransaction trans = new JdbcInterface(global, conn, false);
 * ITest iface = trans.getInterface(ITest.class);
 * </pre>
 */
public class JdbcInterface implements ISimpleTransaction {

    private final TransactionContext transaction;
    private final boolean commitCalls;

    public JdbcInterface(GlobalContext global, Connection connection, boolean commitCalls) {
        this.transaction = new TransactionContext(global, new SingleConnectionManager(connection));
        this.commitCalls = commitCalls;
    }

    public final <T extends IDBCommon> T getInterface(Class<T> iface) {
        return transaction.getInterface(iface, commitCalls);
    }
}
