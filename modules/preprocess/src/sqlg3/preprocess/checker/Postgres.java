package sqlg3.preprocess.checker;

import sqlg3.runtime.Parameter;

import java.sql.Connection;

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
}
