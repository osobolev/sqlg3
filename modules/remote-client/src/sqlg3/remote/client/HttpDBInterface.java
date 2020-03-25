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

    public ISimpleTransaction getAsyncTransaction() {
        return new HttpSimpleTransaction(rootObject, info.id, HttpCommand.INVOKE_ASYNC);
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
            rootObject.httpInvoke(Void.TYPE, HttpCommand.PING, info.id);
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RemoteException(ex);
        }
    }

    public void close() {
        try {
            rootObject.httpInvoke(Void.TYPE, HttpCommand.CLOSE, info.id);
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RemoteException(ex);
        }
    }

    public String getUserLogin() {
        return info.userLogin;
    }

    public String getUserHost() {
        return info.userHost;
    }

    public Object getUserObject() {
        return info.userObject;
    }

    public SessionInfo[] getActiveSessions() {
        try {
            return rootObject.httpInvoke(SessionInfo[].class, HttpCommand.GET_SESSIONS, info.id);
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RemoteException(ex);
        }
    }

    public void killSession(String sessionLongId) {
        try {
            rootObject.httpInvoke(Void.TYPE, HttpCommand.KILL_SESSION, info.id, sessionLongId);
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RemoteException(ex);
        }
    }

    public SessionInfo getCurrentSession() {
        try {
            return rootObject.httpInvoke(SessionInfo.class, HttpCommand.GET_CURRENT_SESSION, info.id);
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RemoteException(ex);
        }
    }
}
