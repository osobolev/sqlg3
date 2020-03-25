package sqlg3.preprocess;

import sqlg3.core.SQLGException;
import sqlg3.runtime.TypeMapper;
import sqlg3.runtime.RuntimeMapper;
import sqlg3.runtime.queries.QueryParser;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.*;
import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * Default {@link Mapper} implementation
 */
public class MapperImpl implements Mapper {

    public Object getTestObject(Class<?> paramType) {
        if (paramType.equals(Integer.TYPE) || paramType.equals(Integer.class)) {
            return 1;
        } else if (paramType.equals(Short.TYPE) || paramType.equals(Short.class)) {
            return (short) 1;
        } else if (paramType.equals(Long.TYPE) || paramType.equals(Long.class)) {
            return 1L;
        } else if (paramType.equals(Byte.TYPE) || paramType.equals(Byte.class)) {
            return (byte) 1;
        } else if (paramType.equals(Float.TYPE) || paramType.equals(Float.class)) {
            return 1.0f;
        } else if (paramType.equals(Double.TYPE) || paramType.equals(Double.class)) {
            return 1.0;
        } else if (paramType.equals(Character.TYPE) || paramType.equals(Character.class)) {
            return '1';
        } else if (paramType.equals(Boolean.TYPE) || paramType.equals(Boolean.class)) {
            return true;
        } else if (BigInteger.class.isAssignableFrom(paramType)) {
            return BigInteger.ONE;
        } else if (BigDecimal.class.isAssignableFrom(paramType)) {
            return BigDecimal.ONE;
        } else if (paramType.equals(String.class)) {
            return "1";
        } else if (Date.class.isAssignableFrom(paramType)) {
            long ts = System.currentTimeMillis();
            try {
                Constructor<?> constructor = paramType.getConstructor(Long.TYPE);
                return constructor.newInstance(ts);
            } catch (Exception ex) {
                // ignore
            }
            return new Date(ts);
        } else if (Temporal.class.isAssignableFrom(paramType)) {
            try {
                Method method = paramType.getMethod("now");
                return method.invoke(null);
            } catch (Exception ex) {
                // ignore
            }
            return null;
        } else if (paramType.isArray()) {
            Class<?> cls = paramType.getComponentType();
            Object array = Array.newInstance(cls, 1);
            Array.set(array, 0, getTestObject(cls));
            return array;
        } else if (paramType.isInterface()) {
            return Proxy.newProxyInstance(paramType.getClassLoader(), new Class<?>[] {paramType}, (proxy, method, args) -> {
                Class<?> retType = method.getReturnType();
                return getTestObject(retType);
            });
        } else if (Enum.class.isAssignableFrom(paramType)) {
            try {
                Method method = paramType.getMethod("values");
                Object[] values = (Object[]) method.invoke(null);
                return values[0];
            } catch (Exception ex) {
                // ignore
            }
            return null;
        } else {
            try {
                Constructor<?> constructor = paramType.getConstructor();
                return constructor.newInstance();
            } catch (Exception ex) {
                // ignore
            }
            return null;
        }
    }

    public enum DecimalType {
        FLOAT, INT, LONG
    }

    protected DecimalType getDecimalType(int scale, int precision) {
        if (scale == -127 || scale > 0 || precision == 0) {
            return DecimalType.FLOAT;
        } else {
            int len = precision - scale;
            if (len >= 20) {
                return DecimalType.FLOAT;
            } else if (len >= 10) {
                return DecimalType.LONG;
            } else {
                return DecimalType.INT;
            }
        }
    }

    /**
     * Defines Java numeric type to use for mapping.
     */
    protected Class<?> getDecimalClass(int scale, int precision, boolean notNull) {
        DecimalType integer = getDecimalType(scale, precision);
        switch (integer) {
        case INT:
            return notNull ? Integer.TYPE : Integer.class;
        case LONG:
            return notNull ? Long.TYPE : Long.class;
        default:
            return notNull ? Double.TYPE : Double.class;
        }
    }

    protected Class<?> getDateClass() {
        return java.sql.Date.class;
    }

    /**
     * Defines Java non-numeric type to use for mapping.
     */
    protected Class<?> getPrimitiveNonDecimalClass(int jdbcType, boolean notNull) {
        switch (jdbcType) {
        case Types.BIGINT:
            if (notNull)
                return Long.TYPE;
            return Long.class;
        case Types.INTEGER:
            if (notNull)
                return Integer.TYPE;
            return Integer.class;
        case Types.SMALLINT:
        case Types.TINYINT:
            if (notNull)
                return Short.TYPE;
            return Short.class;
        case Types.DOUBLE:
            if (notNull)
                return Double.TYPE;
            return Double.class;
        case Types.FLOAT:
        case Types.REAL:
            if (notNull)
                return Float.TYPE;
            return Float.class;
        case Types.DATE:
            return getDateClass();
        case Types.TIME:
        case Types.TIME_WITH_TIMEZONE:
            return Time.class;
        case Types.TIMESTAMP:
        case Types.TIMESTAMP_WITH_TIMEZONE:
            // todo: test Oracle & postgres for timestamp with/without timezone - retrieval/insert
            return Timestamp.class;
        case Types.CHAR:
        case Types.VARCHAR:
        case Types.LONGVARCHAR:
        case Types.NCHAR:
        case Types.NVARCHAR:
        case Types.LONGNVARCHAR:
        case Types.CLOB:
        case Types.NCLOB:
            return String.class;
        case Types.BINARY:
        case Types.VARBINARY:
        case Types.LONGVARBINARY:
        case Types.BLOB:
            return byte[].class;
        case Types.BIT:
        case Types.BOOLEAN:
            if (notNull)
                return Boolean.TYPE;
            return Boolean.class;
        }
        return null;
    }

    /**
     * Returns Java type corresponding to DB column type.
     * @param rsmd result set metadata
     * @param col column index (from 1)
     * @return Java class
     */
    protected Class<?> getJavaType(ResultSetMetaData rsmd, int col) throws SQLException {
        boolean notNull = rsmd.isNullable(col) == ResultSetMetaData.columnNoNulls;
        int jdbcType = rsmd.getColumnType(col);
        switch (jdbcType) {
        case Types.DECIMAL:
        case Types.NUMERIC:
            {
                int precision = rsmd.getPrecision(col);
                int scale = rsmd.getScale(col);
                return getDecimalClass(scale, precision, notNull);
            }
        default:
            {
                Class<?> cls = getPrimitiveNonDecimalClass(jdbcType, notNull);
                if (cls != null) {
                    return cls;
                }
            }
        }
        return Object.class;
    }

    /**
     * Converts column name to Java method name.
     * @param columnName column name
     * @return Java method name
     */
    protected String getFieldName(String columnName) {
        StringBuilder buf = new StringBuilder(columnName.length());
        boolean lower = true;
        for (int i = 0; i < columnName.length(); i++) {
            char c = columnName.charAt(i);
            if (c == '_') {
                lower = false;
                continue;
            }
            if (lower) {
                buf.append(Character.toLowerCase(c));
            } else {
                buf.append(Character.toUpperCase(c));
            }
            lower = true;
        }
        return buf.toString();
    }

    protected String getColumnName(ResultSetMetaData rsmd, int column) throws SQLException {
        return rsmd.getColumnLabel(column);
    }

    /**
     * Defines mapping between simple columns and Java code (field name, type and fetch method).
     */
    protected ColumnInfo getSimpleColumnInfo(ResultSetMetaData rsmd, int j, String columnName) throws SQLException {
        Class<?> type = getJavaType(rsmd, j);
        String name = getFieldName(columnName);
        return new ColumnInfo(type, name);
    }

    public List<ColumnInfo> getFields(ResultSetMetaData rsmd, boolean meta, RuntimeMapper mappers) throws SQLException {
        int count = rsmd.getColumnCount();
        List<ColumnInfo> columns = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            int j = i + 1;
            String columnName = getColumnName(rsmd, j);
            if (columnName.startsWith(QueryParser.SPECIAL)) {
                if (meta)
                    throw new SQLGException("Cannot use custom mapping in meta query");
                String converter = columnName.substring(QueryParser.SPECIAL.length());
                int p = converter.indexOf('#');
                String specialName;
                String specialType;
                if (p >= 0) {
                    specialName = converter.substring(0, p);
                    specialType = converter.substring(p + 1);
                } else {
                    specialName = null;
                    specialType = converter;
                }
                QueryParser.Range range = QueryParser.Range.parseRange(specialType);
                if (range == null)
                    throw new SQLGException("Cannot parse special type " + specialType);
                if (specialName == null) {
                    if (range.from == range.to) {
                        specialName = getColumnName(rsmd, range.from);
                    } else {
                        specialName = range.name;
                    }
                }
                specialName = getFieldName(specialName);
                specialType = range.name;
                Class<?> javaType = getCustomType(specialType);
                if (javaType == null)
                    throw new SQLGException("Cannot map " + specialType);
                TypeMapper<?> mapper = mappers.getMapper(javaType);
                int from = range.from;
                int to = range.to;
                int columnCount = range.to - range.from + 1;
                int required = mapper.getResultSetColumns();
                if (required != columnCount)
                    throw new SQLGException("Wrong number of parameters for " + specialType + ": expected " + required + ", actual " + columnCount);
                columns.set(from - 1, new ColumnInfo(javaType, specialName));
                for (int k = from; k < to; k++) {
                    columns.set(k, null);
                }
            } else {
                columns.add(getSimpleColumnInfo(rsmd, j, columnName));
            }
        }
        columns.removeIf(Objects::isNull);
        return columns;
    }

    /**
     * Defines mapping between custom-mapped columns and Java code (field name and type).
     */
    protected Class<?> getCustomType(String type) {
        return null;
    }

    public Class<?> getParameterClass(Class<?> cls) {
        return cls;
    }
}
