package sqlg3.runtime;

import java.sql.Connection;
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
    long getNextId(Connection conn, String sequence) throws SQLException;
}
