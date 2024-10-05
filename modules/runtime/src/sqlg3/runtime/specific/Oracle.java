package sqlg3.runtime.specific;

import sqlg3.runtime.DBSpecific;

/**
 * {@link DBSpecific} implementation for Oracle.
 */
public final class Oracle implements DBSpecific {

    @Override
    public String getNextIdSql(String sequence) {
        return "SELECT " + sequence + ".NEXTVAL FROM DUAL";
    }
}
