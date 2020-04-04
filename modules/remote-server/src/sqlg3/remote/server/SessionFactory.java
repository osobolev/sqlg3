package sqlg3.remote.server;

import sqlg3.remote.common.IConnectionFactory;
import sqlg3.remote.common.IRemoteDBInterface;
import sqlg3.runtime.ConnectionManager;

import java.sql.SQLException;

public interface SessionFactory {

    final class SessionData {

        /**
         * Connection pool to use for session.
         */
        public final ConnectionManager cman;
        /**
         * Some session object which can be retrieved by client with {@link IRemoteDBInterface#getUserObject()}.
         * Should be serializable.
         */
        public final Object userObject;

        public SessionData(ConnectionManager cman, Object userObject) {
            this.cman = cman;
            this.userObject = userObject;
        }
    }

    /**
     * @param user login user name as in {@link IConnectionFactory#openConnection(String, String)}
     * @param password login password as in {@link IConnectionFactory#openConnection(String, String)}
     * @return session data, always not null
     * @throws SQLException if user/password is not valid or other DB error occured
     */
    SessionData login(SQLGLogger logger, String user, String password) throws SQLException;
}
