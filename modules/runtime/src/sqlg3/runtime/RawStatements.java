package sqlg3.runtime;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public final class RawStatements implements AutoCloseable {

    private final Connection connection;
    private final List<PreparedStatement> toClose = new ArrayList<>();

    RawStatements(Connection connection) {
        this.connection = connection;
    }

    public PreparedStatement prepareStatement(String sql) throws SQLException {
        PreparedStatement stmt = connection.prepareStatement(sql);
        toClose.add(stmt);
        if (GBase.test != null) {
            GBase.test.checkSql(stmt, sql);
        }
        return stmt;
    }

    public CallableStatement prepareCall(String sql) throws SQLException {
        CallableStatement stmt = connection.prepareCall(sql);
        toClose.add(stmt);
        return stmt;
    }

    @Override
    public void close() throws SQLException {
        SQLException error = null;
        for (PreparedStatement stmt : toClose) {
            try {
                stmt.close();
            } catch (SQLException ex) {
                if (error == null) {
                    error = ex;
                } else {
                    error.addSuppressed(ex);
                }
            }
        }
        if (error != null) {
            throw error;
        }
    }
}
