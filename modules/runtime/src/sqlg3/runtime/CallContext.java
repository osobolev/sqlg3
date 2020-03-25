package sqlg3.runtime;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class CallContext {

    private final GlobalContext global;

    private Map<Statement, Parameter[]> statements = null;
    private String lastSql = null;
    private Parameter[] lastParams = null;
    private boolean ok = false;

    private final long t0 = System.currentTimeMillis();

    CallContext(GlobalContext global) {
        this.global = global;
    }

    void setSql(String sql, Parameter[] params) {
        lastSql = sql;
        lastParams = params;
    }

    void statementCreated(Statement stmt, Parameter[] params) {
        if (statements == null) {
            statements = new LinkedHashMap<>();
        }
        statements.put(stmt, params);
    }

    private void trace() {
        long time = System.currentTimeMillis() - t0;
        global.trace.trace(ok, time, () -> {
            List<String> messages = new ArrayList<>();
            if (lastSql != null) {
                messages.add("Last SQL:");
                messages.add(lastSql);
                if (lastParams != null && lastParams.length > 0) {
                    StringBuilder buf = new StringBuilder();
                    buf.append("with params (");
                    for (int i = 0; i < lastParams.length; i++) {
                        if (i > 0) {
                            buf.append(", ");
                        }
                        buf.append(lastParams[i]);
                    }
                    buf.append(")");
                    messages.add(buf.toString());
                }
            }
            return messages;
        });
    }

    Parameter[] getParameters(Statement stmt) {
        return statements.get(stmt);
    }

    private static void close(ResultSet rs, Statement st) {
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException ex) {
                // ignore
            }
        }
        if (st != null) {
            try {
                st.close();
            } catch (SQLException ex) {
                // ignore
            }
        }
    }

    void ok() {
        ok = true;
    }

    void close() {
        trace();
        if (statements != null) {
            for (Statement stmt : statements.keySet()) {
                ResultSet rs = null;
                try {
                    rs = stmt.getResultSet();
                } catch (SQLException ex) {
                    // ignore
                }
                close(rs, stmt);
            }
            statements.clear();
            statements = null;
        }
        lastSql = null;
        lastParams = null;
    }
}
