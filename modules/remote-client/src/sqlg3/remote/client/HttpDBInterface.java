package sqlg3.remote.client;

import sqlg3.core.ISimpleTransaction;
import sqlg3.core.ITransaction;
import sqlg3.remote.common.*;

import java.sql.SQLException;

final class HttpDBInterface implements IRemoteDBInterface {

    private final HttpRootObject rootObject;
    private final HttpDBInterfaceInfo info;

    HttpDBInterface(HttpRootObject rootObject, HttpDBInterfaceInfo info) {
        this.rootObject = rootObject;
        this.info = info;
    }

    public ISimpleTransaction getSimpleTransaction() {
        return new HttpSimpleTransaction(rootObject, info.id, HttpCommand.INVOKE);
    }

    public ITransaction getTransaction() throws SQLException {
        try {
            HttpId transactionId = rootObject.httpInvoke(HttpId.class, HttpCommand.GET_TRANSACTION, info.id);
            return new HttpTransaction(rootObject, transactionId);
        } catch (SQLException | RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RemoteException(ex);
        }
    }

    public void ping() {
        try {
            rootObject.httpInvoke(void.class, HttpCommand.PING, info.id);
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RemoteException(ex);
        }
    }

    public void close() {
        try {
            rootObject.httpInvoke(void.class, HttpCommand.CLOSE, info.id);
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RemoteException(ex);
        }
    }

    public Object getUserObject() {
        return info.userObject;
    }
}
