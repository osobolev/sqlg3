package sqlg3.runtime;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public abstract class GTest {

    public static void setTest(GTest test) {
        GBase.test = test;
    }

    public static GContext testContext(Connection connection, DBSpecific specific, RuntimeMapper mappers) {
        SqlTrace noTrace = (ok, time, getMessages) -> {};
        GlobalContext global = new GlobalContext(specific, mappers, noTrace);
        return new GContext(global, null, connection);
    }

    public abstract Object getTestObject(Class<?> paramType);

    public abstract void getRowTypeFields(Class<?> rowType, ResultSet rs, boolean meta) throws SQLException;

    public abstract void checkOneColumn(ResultSet rs, Class<?> cls) throws SQLException;

    public abstract void checkSql(PreparedStatement stmt, String sql) throws SQLException;

    public abstract Class<?> setParamType(String paramId, Class<?> paramClass);

    public abstract void checkStoredProcName(String procNameToCall, Parameter[] parameters) throws SQLException;

    public abstract void checkSequenceExists(String sequence) throws SQLException;
}
