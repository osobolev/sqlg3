package sqlg3.remote.server;

import sqlg3.core.ISimpleTransaction;
import sqlg3.core.ITransaction;
import sqlg3.remote.common.IRemoteDBInterface;
import sqlg3.remote.common.WatcherThread;
import sqlg3.runtime.GlobalContext;
import sqlg3.runtime.SessionContext;
import sqlg3.runtime.SimpleTransaction;
import sqlg3.runtime.Transaction;

import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicLong;

final class DBInterface implements IRemoteDBInterface {

    private final SessionContext session;
    private final LocalConnectionFactory fact;
    private final GlobalContext global;
    private final SQLGLogger logger;
    private final boolean server;
    private final long sessionOrderId;
    final String sessionLongId;

    private final AtomicLong lastActive = new AtomicLong(getCurrentTime());

    DBInterface(SessionContext session, LocalConnectionFactory fact,
                long sessionOrderId, String sessionLongId, boolean server) {
        this.session = session;
        this.fact = fact;
        this.global = fact.global;
        this.logger = fact.logger;
        this.sessionOrderId = sessionOrderId;
        this.sessionLongId = sessionLongId;
        this.server = server;
        if (LocalConnectionFactory.TRACE) {
            logger.info("Opened " + getConnectionName());
        }
    }

    private String getConnectionName() {
        return server ? "connection" : "local connection";
    }

    public ISimpleTransaction getSimpleTransaction() {
        return new SimpleTransaction(global, session);
    }

    public ITransaction getTransaction() {
        return new Transaction(global, session);
    }

    static long getCurrentTime() {
        return System.currentTimeMillis();
    }

    public void ping() {
        lastActive.set(getCurrentTime());
    }

    void tracePing(String host) {
        if (LocalConnectionFactory.TRACE) {
            logger.trace(":: Ping from " + host);
        }
    }

    private void close(boolean explicit) {
        if (LocalConnectionFactory.TRACE) {
            if (explicit) {
                logger.info("Closing " + getConnectionName());
            } else {
                logger.info("Closing inactive " + getConnectionName());
            }
        }
        try {
            session.close();
        } catch (SQLException ex) {
            logger.error(ex);
        }
        global.fireSessionListeners(listener -> listener.closed(sessionOrderId));
    }

    public void close() {
        close(true);
        fact.endSession(this);
    }

    public Object getUserObject() {
        return session.getUserObject();
    }

    boolean isTimedOut(long time) {
        boolean timeout = time - lastActive.get() >= WatcherThread.ACTIVITY_CHECK_INTERVAL;
        if (timeout) {
            close(false);
        }
        return timeout;
    }
}
