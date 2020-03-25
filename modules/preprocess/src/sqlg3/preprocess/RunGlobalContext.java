package sqlg3.preprocess;

import sqlg3.runtime.GTest;
import sqlg3.runtime.RuntimeMapper;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
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
    private final String runtimeMapperClass;
    private final boolean cleanup;
    private final Path tmpDir;

    final Map<Class<?>, List<RowTypeInfo>> generatedIn = new LinkedHashMap<>();
    final Map<Class<?>, List<RowTypeInfo>> generatedOut = new LinkedHashMap<>();

    private GTestImpl test = null;

    private Path workTmpDir = null;

    RunGlobalContext(Options o) {
        this.driverClass = o.driverClass;
        this.url = o.url;
        this.user = o.user;
        this.pass = o.pass;
        this.checkerClass = o.checkerClass;
        this.mapperClass = o.mapperClass;
        this.runtimeMapperClass = o.runtimeMapperClass;
        this.cleanup = o.cleanup;
        this.tmpDir = o.tmpDir;
    }

    GTestImpl getTest() throws SQLException, ClassNotFoundException, IllegalAccessException, InstantiationException {
        if (test == null) {
            Mapper mapper = (Mapper) Class.forName(mapperClass).newInstance();
            SqlChecker checker;
            RuntimeMapper runtimeMapper;
            try {
                Class.forName(driverClass);
                checker = (SqlChecker) Class.forName(checkerClass).newInstance();
                runtimeMapper = (RuntimeMapper) Class.forName(runtimeMapperClass).newInstance();
            } catch (ClassNotFoundException ex) {
                throw new SQLException(ex);
            }
            Connection connection = DriverManager.getConnection(url, user, pass);
            connection.setAutoCommit(false);
            test = new GTestImpl(connection, checker, mapper, runtimeMapper, generatedIn, generatedOut);
            GTest.setTest(test);
        }
        return test;
    }

    Path getTmpDir() throws IOException {
        if (workTmpDir == null) {
            workTmpDir = Files.createTempDirectory(tmpDir, "sqlg");
        }
        return workTmpDir;
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
        if (cleanup && workTmpDir != null) {
            deleteRecursively(workTmpDir);
        }
    }

    private static void deleteRecursively(Path path) {
        if (Files.isDirectory(path)) {
            try (DirectoryStream<Path> children = Files.newDirectoryStream(path)) {
                for (Path child : children) {
                    deleteRecursively(child);
                }
            } catch (IOException ex) {
                // ignore
            }
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException ex) {
            // ignore
        }
    }
}
