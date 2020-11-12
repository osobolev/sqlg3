package sqlg3.preprocess.checker;

import sqlg3.runtime.Parameter;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * SQL checker for PostgreSQL
 */
public final class Postgres extends Generic {

    public Postgres() {
        super(new sqlg3.runtime.specific.Postgres());
    }

    @Override
    public void checkStoredProcName(Connection conn, String procNameToCall, Parameter[] parameters) {
    }

    @Override
    public void checkSequenceExists(Connection conn, String name) throws SQLException {
        String sql = sqlg3.runtime.specific.Postgres.getNextSeqSql(name);
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            checkSql(conn, stmt, sql);
        }
    }
}
