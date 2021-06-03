package sqlg3.runtime;

import sqlg3.core.IDBCommon;
import sqlg3.core.ITransaction;

import java.sql.SQLException;

public final class Transaction implements ITransaction {

    private final TransactionContext transaction;

    public Transaction(GlobalContext global, SessionContext session) {
        this.transaction = new TransactionContext(global, session);
    }

    @Override
    public <T extends IDBCommon> T getInterface(Class<T> iface) {
        return transaction.getInterface(iface, false, true);
    }

    @Override
    public void commit() throws SQLException {
        transaction.commit();
    }

    @Override
    public void rollback() throws SQLException {
        transaction.rollback();
    }
}
