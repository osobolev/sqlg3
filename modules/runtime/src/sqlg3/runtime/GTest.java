package sqlg3.runtime;

import java.sql.*;

public abstract class GTest {

    public static void setTest(GTest test) {
        GBase.test = test;
    }

    public static GContext testContext(Connection connection, DBSpecific specific, RuntimeMapper mappers) {
        SqlTrace noTrace = (ok, time, getMessages) -> {};
        GlobalContext global = new GlobalContext(specific, mappers, noTrace);
        SessionContext session = new SessionContext(new SingleConnectionManager(connection), null);
        return new GContext(global, session, new TransactionContext(global, session));
    }

    public abstract <T> T getNullInterface(Class<T> iface);

    public abstract Object getTestObject(Class<?> paramType);

    public abstract void getRowTypeFields(Class<?> rowType, ResultSet rs, boolean meta) throws SQLException;

    public abstract void checkOneColumn(ResultSet rs, Class<?> cls) throws SQLException;

    public abstract void checkSql(PreparedStatement stmt) throws SQLException;

    public abstract void statementCreated(Statement stmt, String sql);

    public abstract Class<?> setParamType(String paramId, Class<?> paramClass);

    public abstract void checkStoredProcName(String procNameToCall, Parameter[] parameters) throws SQLException;

    public abstract void checkSequenceExists(String sequence) throws SQLException;
}
