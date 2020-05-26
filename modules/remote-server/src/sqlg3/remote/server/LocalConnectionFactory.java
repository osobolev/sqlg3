package sqlg3.remote.server;

import sqlg3.remote.common.IConnectionFactory;
import sqlg3.remote.common.IRemoteDBInterface;
import sqlg3.remote.common.SessionInfo;
import sqlg3.runtime.GlobalContext;
import sqlg3.runtime.SessionContext;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * {@link IConnectionFactory} implementation for local connection.
 */
public final class LocalConnectionFactory implements IConnectionFactory {

    /**
     * true if output connection open/close information
     */
    public static boolean TRACE = true;

    private static final class SessionRecord {

        final DBInterface db;
        final boolean background;
        final long startTime = DBInterface.getCurrentTime();

        SessionRecord(DBInterface db, boolean background) {
            this.db = db;
            this.background = background;
        }
    }

    private final boolean server;
    private final SessionFactory sessionFactory;
    final SQLGLogger logger;
    final GlobalContext global;

    private final AtomicLong connectionCount = new AtomicLong(0);

    private final Map<String, SessionRecord> connectionMap = new HashMap<>();

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

    private DBInterface newDBInterface(SessionContext session, String user, String host, boolean background) {
        long sessionOrderId = connectionCount.getAndIncrement();
        String sessionLongId = UUID.randomUUID().toString();
        DBInterface lw = new DBInterface(user, host, session, this, sessionOrderId, sessionLongId, server);
        synchronized (this) {
            connectionMap.put(sessionLongId, new SessionRecord(lw, background));
        }
        return lw;
    }

    DBInterface createConnection(String user, String password, String host, boolean background) throws SQLException {
        return openConnection(user, password, host, background);
    }

    private DBInterface openConnection(String user, String password, String host, boolean background) throws SQLException {
        SessionContext session = sessionFactory.login(logger, user, password);
        return newDBInterface(session, user, host, background);
    }

    public IRemoteDBInterface openConnection(String user, String password) throws SQLException {
        return openConnection(user, password, null, false);
    }

    void endSession(DBInterface db) {
        synchronized (this) {
            connectionMap.remove(db.sessionLongId);
        }
    }

    private static SessionInfo getInfo(long time, SessionRecord rec) {
        DBInterface db = rec.db;
        return new SessionInfo(
            db.getUserLogin(), db.getUserHost(), db.sessionOrderId, db.sessionLongId,
            time - rec.startTime, db.getUserObject(), rec.background
        );
    }

    SessionInfo getSessionInfo(DBInterface db) {
        long time = DBInterface.getCurrentTime();
        synchronized (this) {
            SessionRecord rec = connectionMap.get(db.sessionLongId);
            if (rec == null)
                return null;
            return getInfo(time, rec);
        }
    }

    public SessionInfo[] getActiveSessions() {
        long time = DBInterface.getCurrentTime();
        synchronized (this) {
            SessionInfo[] info = new SessionInfo[connectionMap.size()];
            int j = 0;
            for (SessionRecord rec : connectionMap.values()) {
                info[j++] = getInfo(time, rec);
            }
            Arrays.sort(info, Comparator.comparingLong(o -> o.sessionOrderId));
            return info;
        }
    }

    public void killSession(String sessionLongId) {
        SessionRecord rec;
        synchronized (this) {
            rec = connectionMap.get(sessionLongId);
        }
        if (rec != null) {
            rec.db.close();
        }
    }

    DBInterface getSession(String sessionLongId) {
        SessionRecord rec;
        synchronized (this) {
            rec = connectionMap.get(sessionLongId);
        }
        return rec == null ? null : rec.db;
    }

    void checkActivity() {
        long time = DBInterface.getCurrentTime();
        synchronized (this) {
            for (Iterator<SessionRecord> i = connectionMap.values().iterator(); i.hasNext(); ) {
                SessionRecord rec = i.next();
                if (!rec.background) {
                    if (rec.db.isTimedOut(time)) {
                        i.remove();
                    }
                }
            }
        }
    }
}
