package sqlg3.preprocess;

import sqlg3.runtime.DBSpecific;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Used to check validity of SQL queries during preprocess.
 */
public interface SqlChecker {

    /**
     * Returns checker of SQL statements for this DB.
     */
    DBSpecific getSpecific();

    /**
     * Returns current schema name (for stored proc search).
     *
     * @return current schema
     */
    String getCurrentSchema(DatabaseMetaData meta) throws SQLException;

    /**
     * Checks whether sequence generator exists in database.
     *
     * @param name sequence name
     * @throws SQLException when no such sequence
     */
    void checkSequenceExists(Connection conn, String name) throws SQLException;

    /**
     * Checks SQL statement syntax.
     *
     * @throws SQLException when SQL is invalid
     */
    void checkSql(Connection conn, String sql) throws SQLException;

    /**
     * Checks SQL statement syntax.
     *
     * @throws SQLException when SQL is invalid
     */
    void checkStatement(PreparedStatement stmt) throws SQLException;
}
