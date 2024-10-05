package sqlg3.preprocess.checker;

import sqlg3.runtime.queries.QueryParser;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * SQL checker for Oracle
 */
public final class Oracle extends Generic {

    public Oracle() {
        super(new sqlg3.runtime.specific.Oracle());
    }

    private static void checkSql(Connection conn, String sql) throws SQLException {
        String trim = sql.trim().toUpperCase();
        if (trim.startsWith("CREATE") || trim.startsWith("ALTER") || trim.startsWith("DROP"))
            return;
        if (trim.startsWith("{") || trim.startsWith("BEGIN") || trim.startsWith("DECLARE"))
            return;
        String txt =
            "DECLARE\n" +
            "  c NUMBER;\n" +
            "BEGIN\n" +
            "  c := DBMS_SQL.open_cursor;\n" +
            "  DBMS_SQL.parse(c, ?, DBMS_SQL.native);\n" +
            "  DBMS_SQL.close_cursor(c);\n" +
            "EXCEPTION WHEN OTHERS THEN\n" +
            "  DBMS_SQL.close_cursor(c);\n" +
            "  RAISE;\n" +
            "END;";
        try (CallableStatement cs = conn.prepareCall(txt)) {
            cs.setString(1, QueryParser.unparseQuery(sql));
            cs.execute();
        } catch (SQLException ex) {
            throw new SQLException("Invalid SQL: " + sql + "\n" + ex.getMessage(), ex);
        }
    }

    @Override
    public void checkSequenceExists(Connection conn, String name) throws SQLException {
        checkSql(conn, getSpecific().getNextIdSql(name));
    }

    @Override
    public void checkSql(Connection conn, PreparedStatement stmt, String sql) throws SQLException {
        if (sql == null)
            return;
        checkSql(conn, sql);
    }
}
