package sqlg3.runtime;

import sqlg3.core.IDBInterface;
import sqlg3.core.ISimpleTransaction;
import sqlg3.core.ITransaction;

import java.sql.SQLException;

public final class LocalDBInterface implements IDBInterface {

    private final GlobalContext global;
    private final ConnectionManager cman;

    public LocalDBInterface(GlobalContext global, ConnectionManager cman) {
        this.global = global;
        this.cman = cman;
    }

    @Override
    public ISimpleTransaction getSimpleTransaction() {
        return new SimpleTransaction(global, cman);
    }

    @Override
    public ITransaction getTransaction() {
        return new Transaction(global, cman);
    }

    @Override
    public void close() throws SQLException {
        cman.close();
    }
}
