package sqlg3.runtime.specific;

import sqlg3.runtime.DBSpecific;

import java.sql.SQLException;

/**
 * {@link DBSpecific} implementation for generic JDBC database.
 */
public class Generic implements DBSpecific {

    @Override
    public String getNextIdSql(String sequence) throws SQLException {
        throw new SQLException("Database does not support sequences");
    }
}
