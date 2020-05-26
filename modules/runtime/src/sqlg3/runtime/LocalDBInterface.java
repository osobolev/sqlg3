package sqlg3.runtime;

import sqlg3.core.IDBInterface;
import sqlg3.core.ISimpleTransaction;
import sqlg3.core.ITransaction;

import java.sql.SQLException;

public final class LocalDBInterface implements IDBInterface {

    private final GlobalContext global;
    private final SessionContext session;

    public LocalDBInterface(GlobalContext global, ConnectionManager cman) {
        this(global, cman, null);
    }

    public LocalDBInterface(GlobalContext global, ConnectionManager cman, Object userObject) {
        this.global = global;
        this.session = new SessionContext(cman, userObject);
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
