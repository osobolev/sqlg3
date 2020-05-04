package sqlg3.runtime;

import sqlg3.core.Impl;
import sqlg3.core.MetaColumn;
import sqlg3.core.SQLGException;

import java.io.Serializable;
import java.lang.reflect.*;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public final class GlobalContext {

    public static final String ORDER_FIELD = "ORDER";

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

    private static <T> int fetchField(RuntimeMapper mappers, Class<T> fieldType, ResultSet rs, int index,
                                      Consumer<T> value) throws SQLException {
        TypeMapper<T> mapper = mappers.getMapper(fieldType);
        value.accept(mapper.fetch(rs, index));
        return mapper.getResultSetColumns();
    }

    private static void checkRowType(ResultSetMetaData rsmd, int fieldCount) throws SQLException {
        int columnCount = rsmd.getColumnCount();
        if (columnCount != fieldCount) {
            throw new SQLGException(
                "Different number of columns in query (" + columnCount + ") and constructor (" + fieldCount + ")"
            );
        }
    }

    private static int fetchMeta(Class<?> rowType, Class<?> fieldType,
                                 ResultSetMetaData rsmd, int index,
                                 Consumer<MetaColumn> value) throws SQLException {
        if (!MetaColumn.class.equals(fieldType))
            throw new SQLGException("Meta row type should contain only MetaColumns in " + rowType.getCanonicalName());
        value.accept(new MetaColumn(
            rsmd.isNullable(index) == ResultSetMetaData.columnNoNulls,
            rsmd.getColumnDisplaySize(index), rsmd.getPrecision(index), rsmd.getScale(index)
        ));
        return 1;
    }

    private static RowTypeFactory<?> createRowTypeFactory(Class<?> rowType, boolean meta, boolean check) {
        if (rowType.isInterface()) {
            String[] order;
            try {
                Field orderField = rowType.getDeclaredField(ORDER_FIELD);
                order = (String[]) orderField.get(null);
            } catch (IllegalAccessException | NoSuchFieldException | RuntimeException ex) {
                throw new SQLGException("Cannot get fields order for " + rowType.getCanonicalName(), ex);
            }
            Method[] methods = rowType.getDeclaredMethods();
            Map<String, Class<?>> fieldTypeMap = new HashMap<>(methods.length);
            for (Method method : methods) {
                String field = method.getName();
                fieldTypeMap.put(field, method.getReturnType());
            }
            Class<?>[] fieldTypes = new Class[order.length];
            for (int i = 0; i < order.length; i++) {
                String field = order[i];
                fieldTypes[i] = fieldTypeMap.get(field);
            }
            return (mappers, rs) -> {
                int fieldCount = order.length;
                Map<String, Object> rowData = new HashMap<>(fieldCount);
                if (meta) {
                    ResultSetMetaData rsmd = rs.getMetaData();
                    if (check) {
                        checkRowType(rsmd, fieldCount);
                    }
                    int index = 1;
                    for (int i = 0; i < fieldCount; i++) {
                        String field = order[i];
                        index += fetchMeta(
                            rowType, fieldTypes[i], rsmd, index,
                            value -> rowData.put(field, value)
                        );
                    }
                } else {
                    if (check) {
                        checkRowType(rs.getMetaData(), fieldCount);
                    }
                    int index = 1;
                    for (int i = 0; i < fieldCount; i++) {
                        String field = order[i];
                        index += fetchField(
                            mappers, fieldTypes[i], rs, index,
                            value -> rowData.put(field, value)
                        );
                    }
                }
                return Proxy.newProxyInstance(
                    rowType.getClassLoader(), new Class[] {rowType, Serializable.class},
                    (proxy, method, args) -> {
                        String name = method.getName();
                        int paramCount = method.getParameterCount();
                        if ("toString".equals(name) && paramCount == 0) {
                            return rowData.toString();
                        } else if ("hashCode".equals(name) && paramCount == 0) {
                            return System.identityHashCode(proxy);
                        } else if ("equals".equals(name) && paramCount == 1 && Object.class.equals(method.getParameterTypes()[0])) {
                            return proxy == args[0];
                        } else {
                            return rowData.get(name);
                        }
                    }
                );
            };
        }
        List<Constructor<?>> constructors = Arrays.stream(rowType.getConstructors())
            .filter(c -> !c.isSynthetic())
            .collect(Collectors.toList());
        if (constructors.size() != 1)
            throw new SQLGException("Should be only one constructor for " + rowType.getCanonicalName());
        Constructor<?> constructor = constructors.get(0);
        Class<?>[] fieldTypes = constructor.getParameterTypes();
        return (mappers, rs) -> {
            int fieldCount = fieldTypes.length;
            Object[] fields = new Object[fieldCount];
            if (meta) {
                ResultSetMetaData rsmd = rs.getMetaData();
                if (check) {
                    checkRowType(rsmd, fieldCount);
                }
                int index = 1;
                for (int i = 0; i < fieldCount; i++) {
                    int j = i;
                    index += fetchMeta(
                        rowType, fieldTypes[i], rsmd, index,
                        value -> fields[j] = value
                    );
                }
            } else {
                if (check) {
                    checkRowType(rs.getMetaData(), fieldCount);
                }
                int index = 1;
                for (int i = 0; i < fieldCount; i++) {
                    int j = i;
                    index += fetchField(
                        mappers, fieldTypes[i], rs, index,
                        value -> fields[j] = value
                    );
                }
            }
            try {
                return constructor.newInstance(fields);
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
