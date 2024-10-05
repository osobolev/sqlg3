package sqlg3.runtime;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class ConnectionUtil {

    public static Connection openConnection(String driver, String url, String user, String pass) throws SQLException {
        if (driver != null) {
            try {
                Class.forName(driver);
            } catch (ClassNotFoundException ex) {
                throw new SQLException(ex);
            }
        }
        Connection conn = DriverManager.getConnection(url, user, pass);
        try {
            conn.setAutoCommit(false);
        } catch (SQLException ex) {
            try {
                conn.close();
            } catch (SQLException ex2) {
                ex.addSuppressed(ex2);
            }
            throw ex;
        }
        return conn;
    }
}
