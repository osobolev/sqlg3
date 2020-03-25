package sqlg3.preprocess.checker;

import sqlg3.preprocess.SqlChecker;
import sqlg3.runtime.DBSpecific;
import sqlg3.runtime.queries.QueryParser;

import java.sql.*;

/**
 * SQL checker for Oracle
 */
public final class Oracle implements SqlChecker {

    private final DBSpecific oracle = new sqlg3.runtime.specific.Oracle();

    @Override
    public DBSpecific getSpecific() {
        return oracle;
    }

    @Override
    public String getCurrentSchema(DatabaseMetaData meta) throws SQLException {
        return meta.getUserName();
    }

    @Override
    public void checkSequenceExists(Connection conn, String name) throws SQLException {
        checkSql(conn, sqlg3.runtime.specific.Oracle.getNextSeqSql(name));
    }

    @Override
    public void checkSql(Connection conn, String sql) throws SQLException {
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
        }
    }

    @Override
    public void checkStatement(PreparedStatement stmt) {
    }
}
