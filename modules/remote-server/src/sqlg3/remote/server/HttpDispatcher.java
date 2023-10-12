package sqlg3.remote.server;

import sqlg3.core.ISimpleTransaction;
import sqlg3.core.ITransaction;
import sqlg3.remote.common.*;
import sqlg3.runtime.GlobalContext;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.EnumMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Server-side object for HTTP access to business interfaces.
 * Method {@link #dispatch(IHttpRequest)} should be invoked from servlets.
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
    private final WatcherThread watcher;
    private final ConcurrentMap<Long, ITransaction> transactions = new ConcurrentHashMap<>();

    private final AtomicLong transactionCount = new AtomicLong(0);

    private interface HttpAction {

        Object perform(IHttpRequest request, ServerHttpId id, Object[] params) throws Exception;
    }

    private final EnumMap<HttpCommand, HttpAction> actions = new EnumMap<>(HttpCommand.class);

    {
        actions.put(HttpCommand.OPEN, (request, id, params) -> {
            checkApplication(id);
            String user = (String) params[0];
            String password = (String) params[1];
            return openConnection(request, id, user, password);
        });
        actions.put(HttpCommand.GET_TRANSACTION, (request, id, params) -> {
            DBInterface db = checkSession(id);
            ITransaction trans = db.getTransaction();
            long transactionId = transactionCount.getAndIncrement();
            transactions.put(transactionId, trans);
            return request.transaction(id, transactionId);
        });
        actions.put(HttpCommand.PING, (request, id, params) -> {
            DBInterface db = checkSession(id);
            db.ping();
            db.tracePing(request.hostName());
            return null;
        });
        actions.put(HttpCommand.CLOSE, (request, id, params) -> {
            DBInterface db = checkSession(id);
            db.close();
            return null;
        });
        actions.put(HttpCommand.ROLLBACK, (request, id, params) -> {
            if (id.transactionId == null)
                throw new RemoteException("Cannot call method: rollback");
            checkSession(id);
            ITransaction transaction = transactions.get(id.transactionId);
            if (transaction == null)
                throw new RemoteException("Cannot rollback - transaction inactive: " + id.transactionId);
            transaction.rollback();
            freeTransaction(id);
            return null;
        });
        actions.put(HttpCommand.COMMIT, (request, id, params) -> {
            if (id.transactionId == null)
                throw new RemoteException("Cannot call method: commit");
            checkSession(id);
            ITransaction transaction = transactions.get(id.transactionId);
            if (transaction == null)
                throw new RemoteException("Cannot commit - transaction inactive: " + id.transactionId);
            transaction.commit();
            freeTransaction(id);
            return null;
        });
    }

    public HttpDispatcher(String application, SessionFactory sessionFactory, SQLGLogger logger, GlobalContext global) {
        this.application = application;
        this.lw = new LocalConnectionFactory(sessionFactory, logger, global, true);
        this.watcher = new WatcherThread(1, lw::checkActivity);
        this.watcher.runThread();
    }

    private HttpDBInterfaceInfo openConnection(IHttpRequest request, ServerHttpId id, String user, String password) throws SQLException {
        DBInterface db = lw.openConnection(user, password, request.hostName(), request.newSessionId());
        Object userObject = db.getUserObject();
        return new HttpDBInterfaceInfo(request.session(id, db.sessionLongId), userObject);
    }

    private void checkApplication(ServerHttpId id) {
        if (!application.equals(id.application))
            throw new RemoteException("Wrong application");
    }

    private DBInterface checkSession(ServerHttpId id) {
        checkApplication(id);
        if (id.sessionId == null)
            throw new RemoteException("Invalid session");
        DBInterface db = lw.getSession(id.sessionId);
        if (db == null)
            throw new RemoteException("Session closed");
        return db;
    }

    private void freeTransaction(ServerHttpId id) {
        transactions.remove(id.transactionId);
    }

    private void log(Throwable ex) {
        lw.logger.error(ex);
    }

    public Object dispatch(IHttpRequest request, ServerHttpRequest data) throws Throwable {
        Throwable invocationError;
        boolean rethrowSqlExcepion = false;
        try {
            ServerHttpId id = data.id;
            HttpCommand command = data.command;
            if (command == HttpCommand.INVOKE) {
                DBInterface db = checkSession(id);
                Object impl;
                if (id.transactionId != null) {
                    ITransaction transaction = transactions.get(id.transactionId);
                    if (transaction == null)
                        throw new RemoteException("Transaction inactive: " + id.transactionId);
                    impl = transaction.getInterface(data.iface);
                } else {
                    ISimpleTransaction t = db.getSimpleTransaction();
                    impl = t.getInterface(data.iface);
                }
                Method toInvoke = impl.getClass().getMethod(data.method, data.paramTypes);
                try {
                    return toInvoke.invoke(impl, data.params);
                } catch (InvocationTargetException ex) {
                    invocationError = ex.getTargetException();
                }
            } else {
                HttpAction httpAction = actions.get(command);
                rethrowSqlExcepion = command != HttpCommand.PING;
                return httpAction.perform(request, id, data.params);
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
     * Dispatch of HTTP POST request.
     *
     * @param request HTTP request
     */
    public void dispatch(IHttpRequest request) throws IOException {
        ServerHttpRequest data = request.requestData();
        HttpResult httpResult;
        try {
            Object result = dispatch(request, data);
            httpResult = new HttpResult(result, null);
        } catch (Throwable ex) {
            httpResult = new HttpResult(null, ex);
        }
        request.write(httpResult);
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
