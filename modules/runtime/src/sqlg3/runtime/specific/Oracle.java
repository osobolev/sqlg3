package sqlg3.runtime.specific;

import sqlg3.runtime.DBSpecific;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * {@link DBSpecific} implementation for Oracle.
 */
public final class Oracle implements DBSpecific {

    public static String getNextSeqSql(String sequence) {
        return "SELECT " + sequence + ".NEXTVAL FROM DUAL";
    }

    @Override
    public long getNextId(Connection conn, String sequence) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(getNextSeqSql(sequence));
             ResultSet rs = stmt.executeQuery()) {
            rs.next();
            return rs.getLong(1);
        }
    }
}
