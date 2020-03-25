package sqlg3.remote.common;

import java.sql.SQLException;

/**
 * Root interface for DB objects hierarchy - all other objects are produced
 * by IConnectionFactory.
 */
public interface IConnectionFactory {

    /**
     * Connecting to DB using authentication.
     *
     * @param user external user
     * @param password external password
     * @return DB connection
     */
    IRemoteDBInterface openConnection(String user, String password) throws SQLException;
}
