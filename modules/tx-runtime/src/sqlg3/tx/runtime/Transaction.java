package sqlg3.tx.runtime;

import sqlg3.tx.api.IDBCommon;
import sqlg3.tx.api.ITransaction;

import java.sql.SQLException;

public final class Transaction implements ITransaction {

    private final TransactionContext transaction;

    public Transaction(TransGlobalContext global, SessionContext session) {
        this.transaction = new TransactionContext(global, session);
    }

    @Override
    public <T extends IDBCommon> T getInterface(Class<T> iface) {
        return transaction.getInterface(iface, false);
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
