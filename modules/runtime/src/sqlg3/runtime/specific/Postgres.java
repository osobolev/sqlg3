package sqlg3.runtime.specific;

/**
 * {@link sqlg3.runtime.DBSpecific} implementation for PostgreSQL.
 */
public final class Postgres extends Generic {

    @Override
    public String getNextIdSql(String sequence) {
        return "SELECT NEXTVAL('" + sequence + "')";
    }
}
