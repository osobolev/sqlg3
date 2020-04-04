package sqlg3.remote.server;

import sqlg3.core.ISimpleTransaction;
import sqlg3.core.ITransaction;
import sqlg3.remote.common.IRemoteDBInterface;
import sqlg3.remote.common.SessionInfo;
import sqlg3.remote.common.WatcherThread;
import sqlg3.runtime.ConnectionManager;
import sqlg3.runtime.GlobalContext;
import sqlg3.runtime.SimpleTransaction;
import sqlg3.runtime.Transaction;

import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicLong;

final class DBInterface implements IRemoteDBInterface {

    private final ConnectionManager cman;
    private final String userLogin;
    private final String password;
    private final String userHost;
    private final LocalConnectionFactory fact;
    private final GlobalContext global;
    private final SQLGLogger logger;
    private final Object userObject;
    private final boolean server;
    final long sessionOrderId;
    final String sessionLongId;

    private final AtomicLong lastActive = new AtomicLong(getCurrentTime());

    DBInterface(String userLogin, String password, String userHost,
                ConnectionManager cman, LocalConnectionFactory fact,
                Object userObject,
                long sessionOrderId, String sessionLongId, boolean server) {
        this.userLogin = userLogin;
        this.password = password;
        this.userHost = userHost;
        this.cman = cman;
        this.fact = fact;
        this.global = fact.global;
        this.logger = fact.logger;
        this.userObject = userObject;
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
        return new SimpleTransaction(global, cman);
    }

    public ISimpleTransaction getAsyncTransaction() {
        return new AsyncTransaction(this, logger::error);
    }

    public ITransaction getTransaction() {
        return new Transaction(global, cman);
    }

    static long getCurrentTime() {
        return System.currentTimeMillis();
    }

    public void ping() {
        lastActive.set(getCurrentTime());
    }

    void tracePing() {
        if (LocalConnectionFactory.TRACE) {
            logger.trace(":: Ping");
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
            cman.close();
        } catch (SQLException ex) {
            logger.error(ex);
        }
    }

    public void close() {
        close(true);
        fact.endSession(this);
    }

    public String getUserLogin() {
        return userLogin;
    }

    public String getUserHost() {
        return userHost;
    }

    public Object getUserObject() {
        return userObject;
    }

    boolean isTimedOut(long time) {
        boolean timeout = time - lastActive.get() >= WatcherThread.ACTIVITY_CHECK_INTERVAL;
        if (timeout) {
            close(false);
        }
        return timeout;
    }

    public SessionInfo[] getActiveSessions() {
        return fact.getActiveSessions();
    }

    public void killSession(String sessionLongId) {
        fact.killSession(sessionLongId);
    }

    public SessionInfo getCurrentSession() {
        return fact.getSessionInfo(this);
    }

    DBInterface createBackground() throws SQLException {
        return fact.createConnection(userLogin, password, userHost, true);
    }
}
