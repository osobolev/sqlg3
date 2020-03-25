package sqlg3.preprocess.checker;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * SQL checker for PostgreSQL
 */
public final class Postgres extends Generic {

    public Postgres() {
        super(new sqlg3.runtime.specific.Postgres());
    }

    @Override
    public void checkSequenceExists(Connection conn, String name) throws SQLException {
        checkSql(conn, sqlg3.runtime.specific.Postgres.getNextSeqSql(name));
    }
}
