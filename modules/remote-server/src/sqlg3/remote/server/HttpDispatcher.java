package sqlg3.remote.server;

import sqlg3.core.IDBCommon;
import sqlg3.core.ISimpleTransaction;
import sqlg3.core.ITransaction;
import sqlg3.remote.common.*;
import sqlg3.runtime.GlobalContext;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.EnumMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Server-side object for HTTP access to business interfaces.
 * Method {@link #dispatch(String, InputStream, OutputStream)} should be invoked from servlets.
 * <p>
 * Servlet container can have more than one {@link HttpDispatcher} object, and distinguish them by
 * application name (which should be reflected in servlet URL; for example, servlet on
 * /app1/remoting path invokes HttpDispatcher of application app1, etc).
 * <p>
 * {@link HttpDispatcher} object should be created only once for application.
 */
public final class HttpDispatcher {

    private final String application;
    private final LocalConnectionFactory lw;
    private IServerSerializer serializer = new ServerJavaSerializer();
    private final WatcherThread watcher;
    private final ConcurrentMap<Long, ITransaction> transactions = new ConcurrentHashMap<>();

    private final AtomicLong transactionCount = new AtomicLong(0);

    private abstract static class HttpAction {

        protected final boolean hasSqlException;

        protected HttpAction(boolean hasSqlException) {
            this.hasSqlException = hasSqlException;
        }

        abstract Object perform(HttpId id, String hostName, Object... params) throws Exception;
    }

    private final EnumMap<HttpCommand, HttpAction> actions = new EnumMap<>(HttpCommand.class);

    {
        actions.put(HttpCommand.OPEN, new HttpAction(true) {
            Object perform(HttpId id, String hostName, Object... params) throws SQLException {
                checkApplication(id);
                String user = (String) params[0];
                String password = (String) params[1];
                return openConnection(id, user, password, hostName);
            }
        });
        actions.put(HttpCommand.GET_TRANSACTION, new HttpAction(true) {
            Object perform(HttpId id, String hostName, Object... params) {
                DBInterface db = checkSession(id);
                ITransaction trans = db.getTransaction();
                long transactionId = transactionCount.getAndIncrement();
                transactions.put(transactionId, trans);
                return id.createTransaction(transactionId);
            }
        });
        actions.put(HttpCommand.PING, new HttpAction(false) {
            Object perform(HttpId id, String hostName, Object... params) {
                DBInterface db = checkSession(id);
                db.ping();
                db.tracePing();
                return null;
            }
        });
        actions.put(HttpCommand.CLOSE, new HttpAction(true) {
            Object perform(HttpId id, String hostName, Object... params) {
                DBInterface db = checkSession(id);
                db.close();
                return null;
            }
        });
        actions.put(HttpCommand.ROLLBACK, new HttpAction(true) {
            Object perform(HttpId id, String hostName, Object... params) throws SQLException {
                if (id.transactionId == null)
                    throw new RemoteException("Cannot call method: rollback");
                checkSession(id);
                ITransaction transaction = transactions.get(id.transactionId);
                if (transaction == null)
                    throw new RemoteException("Cannot rollback - transaction inactive: " + id.transactionId);
                transaction.rollback();
                freeTransaction(id);
                return null;
            }
        });
        actions.put(HttpCommand.COMMIT, new HttpAction(true) {
            Object perform(HttpId id, String hostName, Object... params) throws SQLException {
                if (id.transactionId == null)
                    throw new RemoteException("Cannot call method: commit");
                checkSession(id);
                ITransaction transaction = transactions.get(id.transactionId);
                if (transaction == null)
                    throw new RemoteException("Cannot commit - transaction inactive: " + id.transactionId);
                transaction.commit();
                freeTransaction(id);
                return null;
            }
        });
    }

    public HttpDispatcher(String application, SessionFactory sessionFactory, SQLGLogger logger, GlobalContext global) {
        this.application = application;
        this.lw = new LocalConnectionFactory(sessionFactory, logger, global, true);
        this.watcher = new WatcherThread(1, lw::checkActivity);
        this.watcher.runThread();
    }

    public void setSerializer(IServerSerializer serializer) {
        this.serializer = serializer;
    }

    private HttpDBInterfaceInfo openConnection(HttpId id, String user, String password, String hostName) throws SQLException {
        DBInterface db = lw.openConnection(user, password, hostName);
        String sessionId = db.sessionLongId;
        Object userObject = db.getUserObject();
        return new HttpDBInterfaceInfo(id.createSession(sessionId), userObject);
    }

    private void checkApplication(HttpId id) {
        if (!application.equals(id.application))
            throw new RemoteException("Wrong application");
    }

    private DBInterface checkSession(HttpId id) {
        checkApplication(id);
        if (id.sessionId == null)
            throw new RemoteException("Invalid session");
        DBInterface db = lw.getSession(id.sessionId);
        if (db == null)
            throw new RemoteException("Session closed");
        return db;
    }

    private void freeTransaction(HttpId id) {
        transactions.remove(id.transactionId);
    }

    private void log(Throwable ex) {
        lw.logger.error(ex);
    }

    public Object dispatch(HttpId id, HttpCommand command,
                           Class<? extends IDBCommon> iface, String method, Class<?>[] paramTypes, Object[] params,
                           String hostName) throws Throwable {
        Throwable invocationError;
        boolean rethrowSqlExcepion = false;
        try {
            DBInterface db = null;
            if (id.sessionId != null) {
                db = checkSession(id);
            }
            if (command == HttpCommand.INVOKE) {
                Object impl;
                if (id.transactionId != null) {
                    ITransaction transaction = transactions.get(id.transactionId);
                    if (transaction == null)
                        throw new RemoteException("Transaction inactive: " + id.transactionId);
                    impl = transaction.getInterface(iface);
                } else {
                    assert db != null;
                    ISimpleTransaction t = db.getSimpleTransaction();
                    impl = t.getInterface(iface);
                }
                Method toInvoke = impl.getClass().getMethod(method, paramTypes);
                try {
                    return toInvoke.invoke(impl, params);
                } catch (InvocationTargetException ex) {
                    invocationError = ex.getTargetException();
                }
            } else {
                HttpAction httpAction = actions.get(command);
                rethrowSqlExcepion = httpAction.hasSqlException;
                return httpAction.perform(id, hostName, params);
            }
        } catch (RemoteException ex) {
            throw ex;
        } catch (Throwable ex) {
            log(ex);
            if (ex instanceof SQLException && rethrowSqlExcepion) {
                throw ex;
            } else {
                throw new RemoteException(ex);
            }
        }
        log(invocationError);
        throw invocationError;
    }

    /**
     * Dispatch of HTTP PUT request.
     *
     * @param hostName host name of client from which call originated
     * @param is       input data
     * @param os       output data
     */
    public void dispatch(String hostName, InputStream is, OutputStream os) throws IOException {
        IServerSerializer.ServerCall call = (id, command, iface, method, paramTypes, params) -> dispatch(id, command, iface, method, paramTypes, params, hostName);
        serializer.serverToClient(is, call, os);
    }

    public static void writeError(IServerSerializer serializer, OutputStream os, Throwable error) throws IOException {
        serializer.sendError(os, error);
    }

    /**
     * Server shutdown
     */
    public void shutdown() {
        watcher.shutdown();
    }

    public String getApplication() {
        return application;
    }
}
