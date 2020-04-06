package sqlg3.runtime;

import sqlg3.core.Impl;
import sqlg3.core.MetaColumn;
import sqlg3.core.SQLGException;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class GlobalContext {

    final DBSpecific db;
    final RuntimeMapper mappers;
    final SqlTrace trace;

    public volatile boolean checkRowTypes = false;
    public final Map<String, Object> userData = new ConcurrentHashMap<>();

    private final ConcurrentMap<Class<?>, RowTypeFactory<?>> rowTypeFactoryCache = new ConcurrentHashMap<>();
    private final ConcurrentMap<Class<?>, ImplCache> implCache = new ConcurrentHashMap<>();

    public GlobalContext(DBSpecific db, RuntimeMapper mappers, SqlTrace trace) {
        this.db = db;
        this.mappers = mappers;
        this.trace = trace;
    }

    private static <T> int fetchParameter(RuntimeMapper mappers, Class<T> parameterType, ResultSet rs, int index,
                                          Object[] params, int i) throws SQLException {
        TypeMapper<T> mapper = mappers.getMapper(parameterType);
        params[i] = mapper.fetch(rs, index);
        return mapper.getResultSetColumns();
    }

    private static void checkRowType(ResultSetMetaData rsmd, Constructor<?> constructor) throws SQLException {
        int columnCount = rsmd.getColumnCount();
        int parameterCount = constructor.getParameterCount();
        if (columnCount != parameterCount) {
            throw new SQLGException(
                "Different number of columns in query (" + columnCount + ") and constructor (" + parameterCount + ")"
            );
        }
    }

    private static RowTypeFactory<?> createRowTypeFactory(Class<?> rowType, boolean meta, boolean check) {
        Constructor<?>[] constructors = rowType.getConstructors();
        if (constructors.length != 1)
            throw new SQLGException("Should be only one constructor for " + rowType.getCanonicalName());
        Constructor<?> constructor = constructors[0];
        Class<?>[] parameterTypes = constructor.getParameterTypes();
        return (mappers, rs) -> {
            Object[] params = new Object[parameterTypes.length];
            if (meta) {
                ResultSetMetaData rsmd = rs.getMetaData();
                if (check) {
                    checkRowType(rsmd, constructor);
                }
                for (int i = 0; i < parameterTypes.length; i++) {
                    if (!MetaColumn.class.equals(parameterTypes[i]))
                        throw new SQLGException("Meta row type should contain only MetaColumns in " + rowType.getCanonicalName());
                    int index = i + 1;
                    params[i] = new MetaColumn(
                        rsmd.isNullable(index) == ResultSetMetaData.columnNoNulls,
                        rsmd.getColumnDisplaySize(index), rsmd.getPrecision(index), rsmd.getScale(index)
                    );
                }
            } else {
                if (check) {
                    checkRowType(rs.getMetaData(), constructor);
                }
                int index = 1;
                for (int i = 0; i < parameterTypes.length; i++) {
                    index += fetchParameter(mappers, parameterTypes[i], rs, index, params, i);
                }
            }
            try {
                return constructor.newInstance(params);
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException ex) {
                throw new SQLGException("Cannot invoke row constructor for " + rowType.getCanonicalName(), ex);
            }
        };
    }

    @SuppressWarnings("unchecked")
    <T> RowTypeFactory<T> getRowTypeFactory(Class<T> rowType, boolean meta) {
        return (RowTypeFactory<T>) rowTypeFactoryCache.computeIfAbsent(rowType, c -> createRowTypeFactory(c, meta, checkRowTypes));
    }

    private static ImplCache createImpl(Class<?> iface) {
        try {
            Impl sqlg = iface.getAnnotation(Impl.class);
            Class<?> dao = iface.getClassLoader().loadClass(sqlg.value());
            Constructor<?> constructor = dao.getConstructor(GContext.class);
            return new ImplCache(dao, constructor);
        } catch (ClassNotFoundException | NoSuchMethodException ex) {
            throw new SQLGException("Cannot find implementation for " + iface.getCanonicalName());
        }
    }

    ImplCache getImpl(Class<?> iface) {
        return implCache.computeIfAbsent(iface, i -> createImpl(iface));
    }
}
