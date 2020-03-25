package sqlg3.runtime;

import sqlg3.core.MetaColumn;
import sqlg3.core.SQLGException;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * For internal use.
 * Caches results of reflection wizardry performed by runtime.
 * todo: move into GlobalContext???
 */
public final class Caches {

    /**
     * Mapping of RowType class to its factory.
     */
    private final ConcurrentMap<Class<?>, RowTypeFactory<?>> rowTypeFactoryMap = new ConcurrentHashMap<>();

    private static <T> int fetchParameter(TypeMappers mappers, Class<T> parameterType, ResultSet rs, int index,
                                          Object[] params, int i) throws SQLException {
        TypeMapper<T> mapper = mappers.getMapper(parameterType);
        params[i] = mapper.fetch(rs, index);
        return mapper.getResultSetColumns();
    }

    private static RowTypeFactory<?> createRowTypeFactory(Class<?> rowType, boolean meta) {
        Constructor<?>[] constructors = rowType.getConstructors();
        if (constructors.length != 1)
            throw new SQLGException("Should be only one constructor for " + rowType.getCanonicalName());
        Constructor<?> constructor = constructors[0];
        Class<?>[] parameterTypes = constructor.getParameterTypes();
        return (mappers, rs) -> {
            Object[] params = new Object[parameterTypes.length];
            if (meta) {
                ResultSetMetaData rsmd = rs.getMetaData();
                // todo: check column names & count
                for (int i = 0; i < parameterTypes.length; i++) {
                    if (!MetaColumn.class.equals(parameterTypes[i]))
                        throw new SQLGException("Meta row type should contain only MetaColumns");
                    int index = i + 1;
                    params[i] = new MetaColumn(
                        rsmd.isNullable(index) == ResultSetMetaData.columnNoNulls,
                        rsmd.getColumnDisplaySize(index), rsmd.getPrecision(index), rsmd.getScale(index)
                    );
                }
            } else {
                // todo: check column names & count
                int index = 1;
                for (int i = 0; i < parameterTypes.length; i++) {
                    index += fetchParameter(mappers, parameterTypes[i], rs, index, params, i);
                }
            }
            try {
                return constructor.newInstance(params);
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException ex) {
                throw new SQLGException("Cannot invoke row constructor", ex);
            }
        };
    }

    @SuppressWarnings("unchecked")
    <T> RowTypeFactory<T> getRowTypeFactory(Class<T> rowType, boolean meta) {
        return (RowTypeFactory<T>) rowTypeFactoryMap.computeIfAbsent(rowType, c -> createRowTypeFactory(c, meta));
    }
}
