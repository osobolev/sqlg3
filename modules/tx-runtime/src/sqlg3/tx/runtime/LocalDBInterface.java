package sqlg3.tx.runtime;

import sqlg3.tx.api.IDBInterface;
import sqlg3.tx.api.ISimpleTransaction;
import sqlg3.tx.api.ITransaction;

import java.sql.SQLException;

public final class LocalDBInterface implements IDBInterface {

    private final TransGlobalContext global;
    private final SessionContext session;

    public LocalDBInterface(TransGlobalContext global, SessionContext session) {
        this.global = global;
        this.session = session;
    }

    @Override
    public ISimpleTransaction getSimpleTransaction() {
        return new SimpleTransaction(global, session);
    }

    @Override
    public ITransaction getTransaction() {
        return new Transaction(global, session);
    }

    @Override
    public void close() throws SQLException {
        session.close();
    }
}
