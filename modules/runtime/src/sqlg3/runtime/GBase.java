package sqlg3.runtime;

import sqlg3.runtime.queries.QueryParser;
import sqlg3.types.SQLGException;

import java.lang.reflect.Proxy;
import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Base class for all classes which are processed by preprocessor. Wraps access to JDBC methods allowing
 * preprocessor to intercept them and extract required information.
 * <p>
 * This class itself is not thread-safe, so it cannot be used by more than
 * one thread at a time. Use wrappers generated by preprocessor to access business methods.
 */
public class GBase {

    /**
     * Use it for {@code autoKeys} parameter of {@link #executeUpdate(QueryLike, String[])} to
     * retrieve all generated columns.
     */
    public static final String[] ALL_KEYS = new String[0];

    static GTest test = null;

    private final GContext ctx;

    /**
     * Constructor. Usually it is called by generated wrappers.
     *
     * @param ctx context
     */
    public GBase(GContext ctx) {
        this.ctx = ctx;
    }

    /**
     * Returns true if method is called at preprocessing time (false at application run time).
     */
    public static boolean isTesting() {
        return test != null;
    }

    private Connection getConnection() throws SQLException {
        return ctx.connection;
    }

    /**
     * Access to raw JDBC connection. Can be used <b>only</b> at application run time, not at preprocess time,
     * so check {@link #isTesting} before calling this method.
     */
    public final Connection getJdbcConnection() throws SQLException {
        if (test != null)
            throw new IllegalStateException("Cannot use Connection in preprocess mode");
        return getConnection();
    }

    public final DatabaseMetaData getMetaData() throws SQLException {
        DatabaseMetaData dbmd = getConnection().getMetaData();
        if (test != null) {
            return (DatabaseMetaData) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class[] {DatabaseMetaData.class},
                (proxy, method, args) -> {
                    if ("getConnection".equals(method.getName()) && method.getParameterCount() == 0) {
                        throw new IllegalStateException("Cannot use Connection in preprocess mode");
                    } else {
                        return method.invoke(dbmd, args);
                    }
                }
            );
        } else {
            return dbmd;
        }
    }

    ///////////////////////////////// Query piece creation /////////////////////////////////

    /**
     * Creates SQL query piece containing query text and its parameters.
     * Example:
     * <pre>
     * QueryPiece piece = query(" AND type_id = ?", in(typeId, Long.class));
     * </pre>
     * It is more convenient to use {@link sqlg3.annotations.Query} annotation to generate such pieces than
     * to use this method manually.
     *
     * @param sql    query text, possibly containing references to parameters in the form of {@code ?}
     * @param params query parameters, see {@link #in}
     */
    public static QueryPiece query(CharSequence sql, Parameter... params) {
        return new QueryPiece(sql, params);
    }

    ///////////////////////////////// Statement preparation /////////////////////////////////

    private interface StatementFactory<S> {

        S create(Connection connection, String sql) throws SQLException;
    }

    private interface StatementExecutor<S, R> {

        R execute(S stmt) throws SQLException;
    }

    private <S extends PreparedStatement, R> R doExecuteAnyStatement(String sql, List<Parameter> params,
                                                                     StatementExecutor<S, R> executor, StatementFactory<S> factory) throws SQLException {
        Connection connection = getConnection();
        long t0 = System.currentTimeMillis();
        boolean ok = false;
        try {
            try (S stmt = factory.create(connection, sql)) {
                Parameter.setParameters(ctx.global.mappers, stmt, params);
                R result = executor.execute(stmt);
                ok = true;
                return result;
            }
        } finally {
            long time = System.currentTimeMillis() - t0;
            ctx.global.trace.trace(ok, time, () -> {
                List<String> messages = new ArrayList<>();
                messages.add("Last SQL:");
                messages.add(sql);
                if (!params.isEmpty()) {
                    StringBuilder buf = new StringBuilder();
                    buf.append("with params (");
                    for (int i = 0; i < params.size(); i++) {
                        if (i > 0) {
                            buf.append(", ");
                        }
                        buf.append(params.get(i));
                    }
                    buf.append(")");
                    messages.add(buf.toString());
                }
                return messages;
            });
        }
    }

    private <R> R doExecuteStatement(String[] autoKeys, QueryLike query, StatementExecutor<PreparedStatement, R> executor) throws SQLException {
        String unparsedSql = query.getSql();
        List<Parameter> params = query.getParameters();
        String parsedSql = QueryParser.parseQuery(unparsedSql);
        if (autoKeys == null) {
            return doExecuteAnyStatement(parsedSql, params, executor, Connection::prepareStatement);
        } else {
            return doExecuteAnyStatement(parsedSql, params, executor, (connection, sql) -> {
                if (autoKeys.length > 0) {
                    DatabaseMetaData meta = connection.getMetaData();
                    Function<String, String> canonicalizer = QueryParser.getCanonicalizer(meta);
                    String[] autoColumns = new String[autoKeys.length];
                    for (int i = 0; i < autoKeys.length; i++) {
                        autoColumns[i] = canonicalizer.apply(autoKeys[i]);
                    }
                    return connection.prepareStatement(sql, autoColumns);
                } else {
                    return connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                }
            });
        }
    }

    private <R> R doExecuteStatement(QueryLike query, StatementExecutor<PreparedStatement, R> executor) throws SQLException {
        return doExecuteStatement(null, query, executor);
    }

    ///////////////////////////////// Raw PreparedStatements /////////////////////////////////

    public final RawStatements raw() throws SQLException {
        return new RawStatements(getConnection());
    }

    /**
     * Binds prepared statement parameters to specific values.
     *
     * @param st SQL statement
     * @param in parameter values
     */
    public final void setParameters(PreparedStatement st, Parameter... in) throws SQLException {
        Parameter.setParameters(ctx.global.mappers, st, Arrays.asList(in));
    }

    /**
     * Binds single prepared statement parameter to specific value.
     *
     * @param st SQL statement
     * @param index index of the parameter (from 1)
     * @param value parameter value
     * @param cls parameter class
     */
    public final <T> void setParameter(PreparedStatement st, int index, T value, Class<T> cls) throws SQLException {
        in(value, cls).set(ctx.global.mappers, st, index);
    }

    public static int executeUpdate(PreparedStatement stmt) throws SQLException {
        if (test != null) {
            return 0;
        } else {
            return stmt.executeUpdate();
        }
    }

    ///////////////////////////////// Setting parameters /////////////////////////////////

    /**
     * For internal use. Do not use this method at runtime, it throws exception when not preprocessing.
     */
    public static Parameter inP(Object value, String paramId) {
        if (test != null) {
            Class<?> cls = test.setParamType(paramId, value.getClass());
            return Parameter.in(value, cls);
        } else {
            throw new SQLGException("No type is defined for parameter " + paramId);
        }
    }

    /**
     * Same as {@link #in} but generated by preprocessor.
     */
    public static <T> Parameter inP(T value, Class<T> cls) {
        return in(value, cls);
    }

    /**
     * Creates parameter for prepared statement.
     *
     * @param value parameter value. Can be null.
     * @param cls   parameter class. Should be not null.
     */
    public static <T> Parameter in(T value, Class<T> cls) {
        return Parameter.in(value, cls);
    }

    /**
     * For internal use. Do not use this method at runtime, it throws exception when not preprocessing.
     */
    public static Parameter outP(Object value, String paramId) {
        if (test != null) {
            if (value == null || !value.getClass().isArray())
                throw new SQLGException("Parameter should be an array");
            test.setParamType(paramId, value.getClass().getComponentType());
            return Parameter.out(value);
        } else {
            throw new SQLGException("No type is defined for out parameter " + paramId);
        }
    }

    /**
     * Same as {@link #out} but generated by preprocessor.
     */
    public static Parameter outP(Object value) {
        return out(value);
    }

    /**
     * Creates OUT parameter for stored procedure call.
     *
     * @param value Should be an array with at least one element to store output value.
     *              Should be not null.
     */
    public static Parameter out(Object value) {
        if (test != null) {
            if (value == null || !value.getClass().isArray())
                throw new SQLGException("Parameter should be an array");
        }
        return Parameter.out(value);
    }

    ///////////////////////////////// Simple value statements /////////////////////////////////

    private static boolean checkNext(ResultSet rs, boolean optional) throws SQLException {
        boolean hasNext = rs.next();
        if (!hasNext) {
            if (optional) {
                return false;
            } else {
                throw new SQLException("No rows found");
            }
        } else {
            return true;
        }
    }

    private static void tooManyRows(ResultSet rs) throws SQLException {
        if (rs.next())
            throw new SQLException("Too many rows");
    }

    private <T> T singleOrOptionalRowQueryReturningT(Class<T> cls, QueryLike query, boolean optional) throws SQLException {
        return doExecuteStatement(query, stmt -> {
            TypeMapper<T> mapper = getMapper(cls);
            try (ResultSet rs = stmt.executeQuery()) {
                if (test != null) {
                    test.checkOneColumn(rs, cls);
                    return cls.cast(test.getTestObject(cls));
                } else {
                    if (!checkNext(rs, optional))
                        return null;
                    T ret = mapper.fetch(rs, 1);
                    tooManyRows(rs);
                    return ret;
                }
            }
        });
    }

    /**
     * Executes select query, which should return one row and one column (more or less than
     * one row raises runtime exception, more or less than one column raises
     * preprocess-time exception).
     *
     * @param cls class with user-defined mapping (see {@link RuntimeMapper})
     */
    public final <T> T singleRowQueryReturning(Class<T> cls, QueryLike query) throws SQLException {
        return singleOrOptionalRowQueryReturningT(cls, query, false);
    }

    /**
     * Executes select query, which should return one row and one column (more or less than
     * one row raises runtime exception, more or less than one column raises
     * preprocess-time exception). Result is returned as a single <code>int</code>. NULLs are returned as zeroes.
     */
    public final int singleRowQueryReturningInt(QueryLike query) throws SQLException {
        Integer value = singleRowQueryReturning(Integer.class, query);
        return value == null ? 0 : value.intValue();
    }

    /**
     * Executes select query, which should return one row and one column (more or less than
     * one row raises runtime exception, more or less than one column raises
     * preprocess-time exception). Result is returned as a single <code>long</code>. NULLs are returned as zeroes.
     */
    public final long singleRowQueryReturningLong(QueryLike query) throws SQLException {
        Long value = singleRowQueryReturning(Long.class, query);
        return value == null ? 0L : value.longValue();
    }

    /**
     * Executes select query, which should return one row and one column (more or less than
     * one row raises runtime exception, more or less than one column raises
     * preprocess-time exception). Result is returned as a single <code>double</code>. NULLs are returned as zeroes.
     */
    public final double singleRowQueryReturningDouble(QueryLike query) throws SQLException {
        Double value = singleRowQueryReturning(Double.class, query);
        return value == null ? 0.0 : value.doubleValue();
    }

    /**
     * Same as {@link #singleRowQueryReturning(Class, QueryLike)} but returns
     * null when no rows found.
     *
     * @param cls class with user-defined mapping (see {@link RuntimeMapper})
     */
    public final <T> T optionalRowQueryReturning(Class<T> cls, QueryLike query) throws SQLException {
        return singleOrOptionalRowQueryReturningT(cls, query, true);
    }

    /**
     * Executes select query returning single column of T.
     *
     * @param cls class with user-defined mapping (see {@link RuntimeMapper})
     */
    public final <T> List<T> columnOf(Class<T> cls, QueryLike query) throws SQLException {
        return doExecuteStatement(query, stmt -> {
            TypeMapper<T> mapper = getMapper(cls);
            List<T> list = new ArrayList<>();
            try (ResultSet rs = stmt.executeQuery()) {
                if (test != null) {
                    test.checkOneColumn(rs, cls);
                } else {
                    while (rs.next()) {
                        list.add(mapper.fetch(rs, 1));
                    }
                }
            }
            return list;
        });
    }

    ///////////////////////////////// Class statements /////////////////////////////////

    private <T> T fetchFromResultSet(Class<T> rowType, ResultSet rs, boolean meta) throws SQLException {
        RowTypeFactory<T> factory = ctx.global.getRowTypeFactory(rowType, meta);
        return factory.fetch(ctx.global.mappers, rs);
    }

    private <T> T singleOrOptionalRowQuery(QueryLike query, boolean optional, Class<T> rowType) throws SQLException {
        return doExecuteStatement(query, stmt -> {
            try (ResultSet rs = stmt.executeQuery()) {
                boolean meta = false;
                if (test != null) {
                    test.getRowTypeFields(rowType, rs, meta);
                    return null;
                } else {
                    if (!checkNext(rs, optional))
                        return null;
                    T ret = fetchFromResultSet(rowType, rs, meta);
                    tooManyRows(rs);
                    return ret;
                }
            }
        });
    }

    /**
     * Executes select query, which should return exactly one row (more or less than
     * one rows raises runtime exception).
     * Result is returned as an object which class implementation is generated by preprocessor.
     *
     * @param query SQL statement
     * @param rowType  row type class or interface generated by preprocessor
     */
    public final <T> T singleRowQuery(QueryLike query, Class<T> rowType) throws SQLException {
        return singleOrOptionalRowQuery(query, false, rowType);
    }

    /**
     * Same as {@link #singleRowQuery(QueryLike, Class)} but returns
     * null when no rows found.
     */
    public final <T> T optionalRowQuery(QueryLike query, Class<T> rowType) throws SQLException {
        return singleOrOptionalRowQuery(query, true, rowType);
    }

    /**
     * Executes select query returning multiple (zero or more) rows.
     * Result is returned as a list of objects which class implementation is generated by preprocessor.
     *
     * @param query SQL statement
     * @param rowType row type class or interface generated by preprocessor
     */
    public final <T> List<T> multiRowQuery(QueryLike query, Class<T> rowType) throws SQLException {
        List<T> result = new ArrayList<>();
        consumeRows(query, rowType, result::add);
        return result;
    }

    /**
     * Converts a result set to a list of objects which class implementation is generated by preprocessor.
     *
     * @param rs result set
     * @param rowType row type class or interface generated by preprocessor
     */
    public final <T> List<T> multiRowQuery(ResultSet rs, Class<T> rowType) throws SQLException {
        List<T> result = new ArrayList<>();
        consumeRows(rs, rowType, result::add);
        return result;
    }

    /**
     * Calls the consumer for each row of the result set.
     *
     * @param rs result set
     * @param rowType row type class or interface generated by preprocessor
     */
    public final <T> void consumeRows(ResultSet rs, Class<T> rowType, Consumer<T> consumer) throws SQLException {
        boolean meta = false;
        if (test != null) {
            test.getRowTypeFields(rowType, rs, meta);
        } else {
            RowTypeFactory<T> factory = ctx.global.getRowTypeFactory(rowType, meta);
            while (rs.next()) {
                T row = factory.fetch(ctx.global.mappers, rs);
                consumer.accept(row);
            }
        }
    }

    /**
     * Executes select query and calls the consumer for each row.
     *
     * @param query SQL statement
     * @param rowType row type class or interface generated by preprocessor
     */
    public final <T> void consumeRows(QueryLike query, Class<T> rowType, Consumer<T> consumer) throws SQLException {
        doExecuteStatement(query, stmt -> {
            try (ResultSet rs = stmt.executeQuery()) {
                consumeRows(rs, rowType, consumer);
            }
            return null;
        });
    }

    /**
     * Returns query ResultSet metadata as RowType object.
     */
    public final <T> T metaRowQuery(ResultSet rs, Class<T> rowType) throws SQLException {
        boolean meta = true;
        if (test != null) {
            test.getRowTypeFields(rowType, rs, meta);
            return null;
        } else {
            return fetchFromResultSet(rowType, rs, meta);
        }
    }

    /**
     * Returns query ResultSet metadata as RowType object.
     */
    public final <T> T metaRowQuery(QueryLike query, Class<T> rowType) throws SQLException {
        return doExecuteStatement(query, stmt -> {
            try (ResultSet rs = stmt.executeQuery()) {
                return metaRowQuery(rs, rowType);
            }
        });
    }

    ///////////////////////////////// Executing DML /////////////////////////////////

    private static int doExecuteUpdate(PreparedStatement stmt, QueryLike query) throws SQLException {
        if (test != null) {
            test.checkSql(stmt, query.getSql());
            return 0;
        } else {
            return stmt.executeUpdate();
        }
    }

    /**
     * Executes update/delete/insert SQL statement. This method should always be used
     * instead of {@link PreparedStatement#executeUpdate()} because the latter can modify
     * database state at preprocess phase.
     *
     * @param query SQL statement
     * @return number of modified database rows
     */
    public int executeUpdate(QueryLike query) throws SQLException {
        return doExecuteStatement(null, query, stmt -> doExecuteUpdate(stmt, query)).intValue();
    }

    /**
     * Executes update or insert SQL statement. This method should always be used
     * instead of {@link PreparedStatement#executeUpdate()} because the latter can modify
     * database state at preprocess phase.
     *
     * @param query SQL statement
     * @param autoKeys generated column names
     * @return update result (number of modified database rows + generated keys)
     */
    public UpdateResult executeUpdate(QueryLike query, String[] autoKeys) throws SQLException {
        return doExecuteStatement(autoKeys, query, stmt -> {
            int rows = doExecuteUpdate(stmt, query);
            Object[] generatedKeys = getGeneratedKeys(stmt);
            return new UpdateResult(rows, generatedKeys);
        });
    }

    /**
     * Executes update or insert SQL statement. This method should always be used
     * instead of {@link PreparedStatement#executeUpdate()} because the latter can modify
     * database state at preprocess phase.
     *
     * @param query SQL statement
     * @param autoKey first generated column name
     * @param otherAutoKeys other generated column names
     * @return update result (number of modified database rows + generated keys)
     */
    public UpdateResult executeUpdate(QueryLike query, String autoKey, String... otherAutoKeys) throws SQLException {
        String[] autoKeys = new String[1 + otherAutoKeys.length];
        autoKeys[0] = autoKey;
        System.arraycopy(otherAutoKeys, 0, autoKeys, 1, otherAutoKeys.length);
        return executeUpdate(query, autoKeys);
    }

    private static Object[] getGeneratedKeys(PreparedStatement stmt) throws SQLException {
        if (test != null) {
            Number[] ret = new Number[10];
            Arrays.fill(ret, 0);
            return ret;
        } else {
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                ResultSetMetaData rsmd = rs.getMetaData();
                int count = rsmd.getColumnCount();
                rs.next();
                Object[] ret = new Number[count];
                for (int i = 0; i < count; i++) {
                    ret[i] = rs.getObject(i + 1);
                }
                return ret;
            }
        }
    }

    ///////////////////////////////// Executing calls /////////////////////////////////

    private static String getProcCallSql(String name, Parameter[] in) {
        StringBuilder buf = new StringBuilder("{ call " + name + "(");
        int argCount = in.length;
        for (int i = 0; i < argCount; i++) {
            if (i > 0)
                buf.append(", ");
            buf.append("?");
        }
        buf.append(") }");
        return buf.toString();
    }

    /**
     * Calls stored procedure.
     * Example:
     * <pre>
     * callStoredProc("trace", in(message, String.class));
     * </pre>
     *
     * @param name   Stored procedure name. SQL statement is generated by procedure name
     *               and parameters as <code>{ call name(in) }</code>.
     * @param params input/output parameters array (see {@link #in} and {@link #out}).
     */
    public final void callStoredProc(String name, Parameter... params) throws SQLException {
        if (test != null) {
            test.checkStoredProcName(name, params);
        } else {
            String sql = getProcCallSql(name, params);
            List<Parameter> paramList = Arrays.asList(params);
            doExecuteAnyStatement(sql, paramList, cs -> {
                cs.execute();
                Parameter.getOutParameters(ctx.global.mappers, cs, paramList);
                return null;
            }, Connection::prepareCall);
        }
    }

    public final void callStoredProc(QueryLike query) throws SQLException {
        List<Parameter> params = query.getParameters();
        doExecuteAnyStatement(query.getSql(), params, cs -> {
            if (test != null) {
                test.checkSql(cs, query.getSql());
            } else {
                cs.execute();
                Parameter.getOutParameters(ctx.global.mappers, cs, params);
            }
            return null;
        }, Connection::prepareCall);
    }

    ///////////////////////////////// Utility methods /////////////////////////////////

    /**
     * Returns next number in sequence.
     *
     * @param sequence sequence name
     */
    public final long getNextId(String sequence) throws SQLException {
        String sql = ctx.global.db.getNextIdSql(sequence);
        if (test != null) {
            try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
                test.checkSql(stmt, sql);
            }
            return 0;
        } else {
            return singleRowQueryReturningLong(query(sql));
        }
    }

    private <T> TypeMapper<T> getMapper(Class<T> cls) {
        return ctx.global.mappers.getMapper(cls);
    }

    public interface RowFetcher<T> {

        T fetchNext() throws SQLException;
    }

    /**
     * Fetches rows from result set.
     *
     * @param rowType row type class
     */
    public final <T> RowFetcher<T> getRowFetcher(Class<T> rowType, ResultSet rs) {
        boolean meta = false;
        if (test != null) {
            return () -> {
                test.getRowTypeFields(rowType, rs, meta);
                return null;
            };
        } else {
            RowTypeFactory<T> factory = ctx.global.getRowTypeFactory(rowType, meta);
            return () -> {
                if (rs.next()) {
                    return factory.fetch(ctx.global.mappers, rs);
                } else {
                    return null;
                }
            };
        }
    }

    public final GContext getContext() {
        return ctx;
    }
}
