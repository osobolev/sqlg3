package sqlg3.remote.client;

import sqlg3.remote.common.HttpCommand;
import sqlg3.remote.common.HttpId;
import sqlg3.remote.common.RemoteException;
import sqlg3.tx.api.ITransaction;

import java.sql.SQLException;

final class HttpTransaction extends HttpSimpleTransaction implements ITransaction {

    HttpTransaction(HttpRootObject rootObject, HttpId id, Object clientContext) {
        super(rootObject, id, clientContext, HttpCommand.INVOKE);
    }

    public void rollback() throws SQLException {
        try {
            rootObject.httpInvoke(void.class, clientContext, HttpCommand.ROLLBACK, id);
        } catch (SQLException | RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RemoteException(ex);
        }
    }

    public void commit() throws SQLException {
        try {
            rootObject.httpInvoke(void.class, clientContext, HttpCommand.COMMIT, id);
        } catch (SQLException | RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RemoteException(ex);
        }
    }
}
