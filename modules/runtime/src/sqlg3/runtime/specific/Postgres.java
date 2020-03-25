package sqlg3.runtime.specific;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * {@link sqlg3.runtime.DBSpecific} implementation for PostgreSQL.
 */
public final class Postgres extends Generic {

    public static String getNextSeqSql(String sequence) {
        return "SELECT NEXTVAL('" + sequence + "')";
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
