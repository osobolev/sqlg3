package sqlg3.tx.runtime;

import sqlg3.tx.api.IDBCommon;
import sqlg3.tx.api.ISimpleTransaction;

public final class SimpleTransaction implements ISimpleTransaction {

    private final TransactionContext transaction;

    public SimpleTransaction(TransGlobalContext global, SessionContext session) {
        this.transaction = new TransactionContext(global, session);
    }

    @Override
    public <T extends IDBCommon> T getInterface(Class<T> iface) {
        return transaction.getInterface(iface, true);
    }
}
