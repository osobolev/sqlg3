package sqlg3.runtime;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Connection pool interface. Pool is created once for each connected user.
 */
public interface ConnectionManager {

    /**
     * Error message ({@link SQLException#getMessage()})
     * for "JDBC driver not found"
     */
    String DRIVER_NOT_FOUND = "JDBC driver not found";

    /**
     * Connection allocation.
     *
     */
    Connection allocConnection() throws SQLException;

    /**
     * Connection release.
     */
    void releaseConnection(Connection conn) throws SQLException;

    /**
     * Commits transaction on business method finish (for simple transactions)/on commit for full transactions.
     */
    void commit(Connection conn) throws SQLException;

    /**
     * Rolls back transaction on business method fail (for simple transactions)/on rollback for full transactions.
     */
    void rollback(Connection conn) throws SQLException;

    /**
     * Closing all connections and releasing all resources.
     */
    void close() throws SQLException;
}
