package sqlg3.runtime;

import java.sql.SQLException;

/**
 * Database-specific operations interface.
 */
public interface DBSpecific {

    /**
     * Gets next number from sequence
     *
     * @param sequence sequence name
     */
    String getNextIdSql(String sequence) throws SQLException;
}
