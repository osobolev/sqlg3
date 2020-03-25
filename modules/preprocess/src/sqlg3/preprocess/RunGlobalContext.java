package sqlg3.preprocess;

import sqlg3.runtime.GTest;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class RunGlobalContext implements AutoCloseable {

    private final String driverClass;
    private final String url;
    private final String user;
    private final String pass;
    private final String checkerClass;
    private final String mapperClass;

    final Map<Class<?>, List<RowTypeInfo>> generatedIn = new LinkedHashMap<>();
    final Map<Class<?>, List<RowTypeInfo>> generatedOut = new LinkedHashMap<>();

    private GTestImpl test = null;

    RunGlobalContext(String driverClass, String url, String user, String pass, String checkerClass, String mapperClass) {
        this.driverClass = driverClass;
        this.url = url;
        this.user = user;
        this.pass = pass;
        this.checkerClass = checkerClass;
        this.mapperClass = mapperClass;
    }

    GTestImpl getTest() throws SQLException, ClassNotFoundException, IllegalAccessException, InstantiationException {
        if (test == null) {
            Mapper mapper = (Mapper) Class.forName(mapperClass).newInstance();
            SqlChecker checker;
            try {
                Class.forName(driverClass);
                checker = (SqlChecker) Class.forName(checkerClass).newInstance();
            } catch (ClassNotFoundException ex) {
                throw new SQLException(ex);
            }
            Connection connection = DriverManager.getConnection(url, user, pass);
            connection.setAutoCommit(false);
            test = new GTestImpl(connection, checker, mapper, generatedIn, generatedOut);
            GTest.setTest(test);
        }
        return test;
    }

    @Override
    public void close() {
        if (test != null) {
            try {
                test.connection.close();
            } catch (SQLException ex) {
                // ignore
            }
            if (url.startsWith("jdbc:derby:")) {
                // Special case for Derby:
                try {
                    DriverManager.getConnection("jdbc:derby:;shutdown=true");
                } catch (SQLException ex) {
                    // ignore
                }
            }
        }
    }
}
