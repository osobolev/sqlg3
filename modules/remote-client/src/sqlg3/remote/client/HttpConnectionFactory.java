package sqlg3.remote.client;

import sqlg3.remote.common.*;

import java.net.*;
import java.sql.SQLException;

/**
 * {@link IConnectionFactory} implementation for remote calls.
 */
public final class HttpConnectionFactory implements IConnectionFactory {

    private final HttpId id;
    private final HttpRootObject rootObject;

    public HttpConnectionFactory(String url, Proxy proxy, String application) throws URISyntaxException, MalformedURLException {
        this(new URI(url).normalize().toURL(), proxy, application);
    }

    public HttpConnectionFactory(URL url, Proxy proxy, String application) {
        this(application, () -> DefaultHttpClient.create(url, proxy, 3000));
    }

    public HttpConnectionFactory(String application, IHttpClientFactory clientFactory) {
        this.id = new HttpId(application);
        this.rootObject = new HttpRootObject(clientFactory);
    }

    public void setSerializer(IClientSerializer serializer) {
        rootObject.setSerializer(serializer);
    }

    public IRemoteDBInterface openConnection(String user, String password) throws SQLException {
        try {
            HttpDBInterfaceInfo info = rootObject.httpInvoke(HttpDBInterfaceInfo.class, HttpCommand.OPEN, id, user, password);
            return new HttpDBInterface(rootObject, info);
        } catch (SQLException | RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RemoteException(ex);
        }
    }
}
