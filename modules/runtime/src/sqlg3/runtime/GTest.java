package sqlg3.runtime;

import sqlg3.core.SQLGLogger;

import java.sql.*;

public abstract class GTest {

    public static void setTest(GTest test) {
        GBase.test = test;
    }

    public static GContext testContext(Connection connection, DBSpecific specific, RuntimeMapper mappers) {
        SqlTrace noTrace = (ok, time, getMessages) -> {};
        GlobalContext global = new GlobalContext(new SQLGLogger.Simple(), specific, mappers, noTrace);
        return new GContext(
            global,
            new TransactionContext(global, new SingleConnectionManager(connection))
        );
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
