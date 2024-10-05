package sqlg3.preprocess;

import sqlg3.runtime.DBSpecific;
import sqlg3.runtime.Parameter;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Used to check validity of SQL queries during preprocess.
 */
public interface SqlChecker {

    DBSpecific getSpecific();

    // todo: remove it!!!
    void checkSequenceExists(Connection conn, String name) throws SQLException;

    void checkStoredProcName(Connection conn, String procNameToCall, Parameter[] parameters) throws SQLException;

    /**
     * Checks SQL statement syntax.
     *
     * @param sql can be null
     */
    void checkSql(Connection conn, PreparedStatement stmt, String sql) throws SQLException;
}
