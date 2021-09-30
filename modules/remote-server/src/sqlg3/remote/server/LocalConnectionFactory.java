package sqlg3.remote.server;

import sqlg3.remote.common.IConnectionFactory;
import sqlg3.remote.common.IRemoteDBInterface;
import sqlg3.runtime.GlobalContext;
import sqlg3.runtime.SessionContext;

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
    final GlobalContext global;

    private final AtomicLong connectionCount = new AtomicLong(0);

    private final Map<String, DBInterface> connectionMap = new HashMap<>();

    /**
     * Constructor.
     */
    public LocalConnectionFactory(SessionFactory sessionFactory, SQLGLogger logger, GlobalContext global) {
        this(sessionFactory, logger, global, false);
    }

    LocalConnectionFactory(SessionFactory sessionFactory, SQLGLogger logger, GlobalContext global, boolean server) {
        this.sessionFactory = sessionFactory;
        this.logger = logger;
        this.global = global;
        this.server = server;
    }

    DBInterface openConnection(String user, String password, String host) throws SQLException {
        long sessionOrderId = connectionCount.getAndIncrement();
        SessionContext session = sessionFactory.login(logger, sessionOrderId, user, password);
        global.fireSessionListeners(listener -> listener.opened(sessionOrderId, user, host, session.getUserObject()));
        String sessionLongId = UUID.randomUUID().toString();
        DBInterface lw = new DBInterface(session, this, sessionOrderId, sessionLongId, server);
        synchronized (this) {
            connectionMap.put(sessionLongId, lw);
        }
        return lw;
    }

    public IRemoteDBInterface openConnection(String user, String password) throws SQLException {
        return openConnection(user, password, null);
    }

    void endSession(DBInterface db) {
        synchronized (this) {
            connectionMap.remove(db.sessionLongId);
        }
    }

    DBInterface getSession(String sessionLongId) {
        synchronized (this) {
            return connectionMap.get(sessionLongId);
        }
    }

    void checkActivity() {
        long time = DBInterface.getCurrentTime();
        synchronized (this) {
            connectionMap.values().removeIf(db -> db.isTimedOut(time));
        }
    }
}
