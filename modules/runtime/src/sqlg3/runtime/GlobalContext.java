package sqlg3.runtime;

import sqlg3.types.MetaColumn;
import sqlg3.types.SQLGException;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public final class GlobalContext {

    final DBSpecific db;
    final RuntimeMapper mappers;
    final SqlTrace trace;

    public volatile boolean checkRowTypes = false;
    public final Map<String, Object> userData = new ConcurrentHashMap<>();

    private final ConcurrentMap<Class<?>, RowTypeFactory<?>> rowTypeFactoryCache = new ConcurrentHashMap<>();
    private final Map<Class<?>, Constructor<?>> constructorCache = new ConcurrentHashMap<>();

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

    public static Constructor<?> getDaoConstructor(Class<?> cls) {
        try {
            return cls.getConstructor(GContext.class);
        } catch (NoSuchMethodException ex) {
            throw new SQLGException("Cannot find GContext constructor for " + cls.getCanonicalName());
        }
    }

    public Object newDaoInstance(Class<?> cls, Connection connection, Object userObject) throws Exception {
        Constructor<?> constructor = constructorCache.computeIfAbsent(cls, GlobalContext::getDaoConstructor);
        return constructor.newInstance(new GContext(this, userObject, connection));
    }
}
