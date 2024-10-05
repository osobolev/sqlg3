package sqlg3.tx.api;

import java.sql.SQLException;

/**
 * Main DB interface class - DB connection abstraction.
 * Can be viewed as a {@link java.sql.Connection} analog,
 * while really it can be a whole pool of connections.
 */
public interface IDBInterface extends AutoCloseable {

    /**
     * Creates "simple" transaction.
     * @see ISimpleTransaction
     */
    ISimpleTransaction getSimpleTransaction() throws SQLException;

    /**
     * Creates transaction.
     * @see ITransaction
     */
    ITransaction getTransaction() throws SQLException;

    /**
     * Closes DB connection.
     */
    void close() throws SQLException;
}
