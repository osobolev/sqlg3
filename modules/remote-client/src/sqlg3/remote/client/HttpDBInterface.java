package sqlg3.remote.client;

import sqlg3.remote.common.*;
import sqlg3.tx.api.ISimpleTransaction;
import sqlg3.tx.api.ITransaction;

import java.sql.SQLException;

final class HttpDBInterface implements IRemoteDBInterface {

    private final HttpRootObject rootObject;
    private final HttpDBInterfaceInfo info;
    private final Object clientContext;

    HttpDBInterface(HttpRootObject rootObject, HttpDBInterfaceInfo info, Object clientContext) {
        this.rootObject = rootObject;
        this.info = info;
        this.clientContext = clientContext;
    }

    public ISimpleTransaction getSimpleTransaction() {
        return new HttpSimpleTransaction(rootObject, info.id, clientContext, HttpCommand.INVOKE);
    }

    public ITransaction getTransaction() throws SQLException {
        try {
            HttpId transactionId = rootObject.httpInvoke(HttpId.class, clientContext, HttpCommand.GET_TRANSACTION, info.id);
            return new HttpTransaction(rootObject, transactionId, clientContext);
        } catch (SQLException | RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RemoteException(ex);
        }
    }

    public void ping() {
        try {
            rootObject.httpInvoke(void.class, clientContext, HttpCommand.PING, info.id);
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RemoteException(ex);
        }
    }

    public void close() {
        try {
            rootObject.httpInvoke(void.class, clientContext, HttpCommand.CLOSE, info.id);
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
