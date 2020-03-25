package sqlg3.preprocess.checker;

import sqlg3.preprocess.SqlChecker;
import sqlg3.runtime.DBSpecific;

import java.sql.*;

/**
 * Base SQL checker for generic JDBC database - does not support sequences.
 */
public class Generic implements SqlChecker {

    private final DBSpecific specific;

    public Generic(DBSpecific specific) {
        this.specific = specific;
    }

    public Generic() {
        this(new sqlg3.runtime.specific.Generic());
    }

    @Override
    public DBSpecific getSpecific() {
        return specific;
    }

    @Override
    public String getCurrentSchema(DatabaseMetaData meta) throws SQLException {
        return meta.getUserName();
    }

    @Override
    public void checkSequenceExists(Connection conn, String name) throws SQLException {
        throw new SQLException("Database does not support sequences");
    }

    @Override
    public void checkSql(Connection conn, String sql) throws SQLException {
        String trim = sql.trim().toUpperCase();
        if (trim.startsWith("CREATE") || trim.startsWith("ALTER") || trim.startsWith("DROP"))
            return;
        if (trim.startsWith("{")) {
            try (CallableStatement cs = conn.prepareCall(sql)) {
                checkStatement(cs);
            }
            // ignore
        } else {
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                checkStatement(ps);
            }
            // ignore
        }
    }

    @Override
    public void checkStatement(PreparedStatement stmt) throws SQLException {
        stmt.getParameterMetaData();
    }
}
