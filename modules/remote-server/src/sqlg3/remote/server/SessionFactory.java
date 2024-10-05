package sqlg3.remote.server;

import sqlg3.remote.common.IConnectionFactory;
import sqlg3.tx.runtime.SessionContext;

import java.sql.SQLException;

public interface SessionFactory {

    /**
     * @param user login user name as in {@link IConnectionFactory#openConnection(String, String)}
     * @param password login password as in {@link IConnectionFactory#openConnection(String, String)}
     * @return session data, always not null
     * @throws SQLException if user/password is not valid or other DB error occured
     */
    SessionContext login(SQLGLogger logger, long sessionId, String user, String password) throws SQLException;
}
