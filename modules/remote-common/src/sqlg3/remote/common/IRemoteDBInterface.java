package sqlg3.remote.common;

import sqlg3.core.IDBInterface;

/**
 * Main DB interface class - DB connection abstraction.
 * Can be viewed as a {@link java.sql.Connection} analog,
 * while really it can be a whole pool of connections.
 */
public interface IRemoteDBInterface extends IDBInterface {

    /**
     * Checks server for availability and signal that client is alive.
     */
    void ping();

    /**
     * Returns user login (which was passed to {@link IConnectionFactory#openConnection(String, String)}).
     *
     * @return user login
     */
    String getUserLogin();

    /**
     * User host name.
     *
     * @return host name
     */
    String getUserHost();

    /**
     * Returns user identifier object.
     */
    Object getUserObject();

    /**
     * Returns active sessions descriptors.
     */
    SessionInfo[] getActiveSessions();

    void killSession(String sessionLongId);

    /**
     * Returns calling session descriptor
     */
    SessionInfo getCurrentSession();
}
