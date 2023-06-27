package sqlg3.remote.client;

import sqlg3.remote.common.*;

import java.sql.SQLException;

/**
 * {@link IConnectionFactory} implementation for remote calls.
 */
public final class HttpConnectionFactory implements IConnectionFactory {

    private final HttpId id;
    private final HttpRootObject rootObject;

    public HttpConnectionFactory(String application, IHttpClient client) {
        this.id = new HttpId(application);
        this.rootObject = new HttpRootObject(client);
    }

    public IRemoteDBInterface openConnection(String user, String password) throws SQLException {
        try {
            Object clientContext = rootObject.newContext();
            HttpDBInterfaceInfo info = rootObject.httpInvoke(HttpDBInterfaceInfo.class, clientContext, HttpCommand.OPEN, id, user, password);
            return new HttpDBInterface(rootObject, info, clientContext);
        } catch (SQLException | RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RemoteException(ex);
        }
    }
}
