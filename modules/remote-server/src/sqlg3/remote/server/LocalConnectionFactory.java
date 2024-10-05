package sqlg3.remote.server;

import sqlg3.remote.common.IConnectionFactory;
import sqlg3.remote.common.IRemoteDBInterface;
import sqlg3.tx.runtime.SessionContext;
import sqlg3.tx.runtime.TransGlobalContext;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * {@link IConnectionFactory} implementation for local connection.
 */
public final class LocalConnectionFactory implements IConnectionFactory {

    /**
     * true if output connection open/close information
     */
    public static boolean TRACE = true;

    private final boolean server;
    private final SessionFactory sessionFactory;
    final SQLGLogger logger;
    final TransGlobalContext global;

    private final AtomicLong connectionCount = new AtomicLong(0);

    private final Map<String, DBInterface> connectionMap = new HashMap<>();

    /**
     * Constructor.
     */
    public LocalConnectionFactory(SessionFactory sessionFactory, SQLGLogger logger, TransGlobalContext global) {
        this(sessionFactory, logger, global, false);
    }

    LocalConnectionFactory(SessionFactory sessionFactory, SQLGLogger logger, TransGlobalContext global, boolean server) {
        this.sessionFactory = sessionFactory;
        this.logger = logger;
        this.global = global;
        this.server = server;
    }

    DBInterface openConnection(String user, String password, String host, String newSessionId) throws SQLException {
        long sessionOrderId = connectionCount.getAndIncrement();
        SessionContext session = sessionFactory.login(logger, sessionOrderId, user, password);
        global.fireSessionListeners(listener -> listener.opened(sessionOrderId, user, host, session.getUserObject()));
        String sessionLongId;
        if (newSessionId == null) {
            sessionLongId = UUID.randomUUID().toString();
        } else {
            sessionLongId = newSessionId;
        }
        DBInterface lw = new DBInterface(session, this, sessionOrderId, sessionLongId, server);
        synchronized (connectionMap) {
            connectionMap.put(sessionLongId, lw);
        }
        return lw;
    }

    public IRemoteDBInterface openConnection(String user, String password) throws SQLException {
        return openConnection(user, password, null, null);
    }

    void endSession(DBInterface db) {
        synchronized (connectionMap) {
            connectionMap.remove(db.sessionLongId);
        }
    }

    DBInterface getSession(String sessionLongId) {
        synchronized (connectionMap) {
            return connectionMap.get(sessionLongId);
        }
    }

    void checkActivity() {
        long time = DBInterface.getCurrentTime();
        synchronized (connectionMap) {
            connectionMap.values().removeIf(db -> db.isTimedOut(time));
        }
    }
}
